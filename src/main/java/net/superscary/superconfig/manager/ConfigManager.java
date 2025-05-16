package net.superscary.superconfig.manager;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.value.AbstractValue;
import net.superscary.superconfig.value.ConfigValue;
import net.superscary.superconfig.value.wrappers.ListValue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ConfigManager<T> {
	private final Class<T> type;
	private final Path file;
	private final ObjectMapper mapper;

	public ConfigManager (Class<T> type, Path file) {
		this.type = type;
		this.file = file;

		// build a JsonFactory that accepts JSON5-style extensions
		JsonFactory factory = JsonFactory.builder()
				.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)                // // and /*…*/
				.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)               // 'foo'
				.enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)        // foo: 1
				.enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)   // 007
				.enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS) // .5
				.enable(JsonReadFeature.ALLOW_TRAILING_COMMA)              // [1,2,]
				.build();

		this.mapper = new ObjectMapper(factory)
				.enable(SerializationFeature.INDENT_OUTPUT);
	}

	/**
	 * Load (or create) the config instance
	 */
	public T load () throws IOException, IllegalAccessException {
		T cfg = instantiate(type);
		if (Files.exists(file)) {
			JsonNode root = mapper.readTree(Files.newBufferedReader(file));
			populate(cfg, root);
		}
		return cfg;
	}

	/**
	 * Write out with comments
	 */
	public void save (T config) throws IOException, IllegalAccessException {
		try (BufferedWriter w = Files.newBufferedWriter(file,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			writeObject(config, w, 0);
		}
	}

	// ——— Internals ———

	private <U> U instantiate (Class<U> cls) {
		try {
			return cls.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to instantiate " + cls.getName(), e);
		}
	}

	private void populate (Object obj, JsonNode node) throws IOException, IllegalAccessException {
		for (Field f : obj.getClass().getDeclaredFields()) {
			f.setAccessible(true);
			String key = f.getName().toLowerCase();
			JsonNode child = node.get(key);

			// 1) Nested @Config objects
			if (f.getType().isAnnotationPresent(Config.class)) {
				Object nested = f.get(obj);
				if (nested == null) {
					nested = instantiate(f.getType());
					f.set(obj, nested);
				}
				if (child != null && child.isObject()) {
					populate(nested, child);
				}
				continue;
			}

			// 2) ListValue<T>
			if (ListValue.class.isAssignableFrom(f.getType())) {
				if (child == null || !child.isArray()) continue;

				// 1) figure out declared T in ListValue<T>
				Type gt = f.getGenericType();
				if (!(gt instanceof ParameterizedType pt)) {
					throw new IOException("Missing generic type for ListValue on field " + f.getName());
				}
				Type arg = pt.getActualTypeArguments()[0];
				if (!(arg instanceof Class<?> declaredElem)) {
					throw new IOException("Cannot handle generic type " + arg + " on field " + f.getName());
				}

				// 2) unwrap wrappers like IntegerValue → Integer.class
				Class<?> elemType = declaredElem;
				if (ConfigValue.class.isAssignableFrom(declaredElem)) {
					elemType = unwrapValueType(declaredElem);
				}

				// 3) build the list manually, element by element
				List<Object> built = new ArrayList<>();
				for (JsonNode elNode : child) {
					Object element;

					// 3a) nested @Config object?
					if (elemType.isAnnotationPresent(Config.class)) {
						element = instantiate(elemType);
						populate(element, elNode);
					}
					// 3b) primitive / enum / POJO
					else {
						if (elNode.isValueNode()) {
							// e.g. a plain number, string, boolean
							element = mapper.treeToValue(elNode, elemType);
						}
						else if (elNode.isObject()) {
							// maybe old‐style wrapper object? try to pull "value" field
							JsonNode valNode = elNode.get("value");
							if (valNode != null && valNode.isValueNode()) {
								element = mapper.treeToValue(valNode, elemType);
							} else {
								// last resort: try binding the whole object to elemType
								element = mapper.convertValue(elNode, elemType);
							}
						}
						else {
							// arrays or other structures—let Jackson handle
							element = mapper.convertValue(elNode, elemType);
						}
					}

					built.add(element);
				}

				// 4) set on your ListValue instance
				Object raw = f.get(obj);
				if (raw instanceof ListValue<?> lv) {
					@SuppressWarnings("unchecked")
					ListValue<Object> listVal = (ListValue<Object>) lv;
					listVal.set(built);
				} else {
					throw new IllegalStateException("Field " + f.getName()
							+ " is not a ListValue: " + raw.getClass());
				}

				continue;
			}

			// 3) Scalar ConfigValue<V>
			if (ConfigValue.class.isAssignableFrom(f.getType())) {
				@SuppressWarnings("unchecked")
				ConfigValue<Object> cv = (ConfigValue<Object>) f.get(obj);
				if (child != null && !child.isNull()) {
					Object v = mapper.treeToValue(child, inferGenericType(f));
					cv.set(v);
				}
			}
		}
	}

	private Class<?> inferGenericType (Field f) {
		// 1) if it's parameterized (e.g. EnumValue<MyMode>, ListValue<String>, etc.)
		Type gt = f.getGenericType();
		if (gt instanceof ParameterizedType pt) {
			Type[] args = pt.getActualTypeArguments();
			if (args.length == 1 && args[0] instanceof Class<?> argClass) {
				// Enum support
				if (Enum.class.isAssignableFrom(argClass)) {
					@SuppressWarnings("unchecked")
					Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) argClass;
					return enumType;
				}
				// primitive wrappers or String
				return argClass;
			}
		}

		// 2) fallback on field type name (if you still have non-generic ConfigValue subclasses)
		return switch (f.getType().getSimpleName()) {
			case "BooleanValue" -> Boolean.class;
			case "IntegerValue" -> Integer.class;
			case "StringValue" -> String.class;
			case "DoubleValue" -> Double.class;
			case "LongValue" -> Long.class;
			case "FloatValue" -> Float.class;
			case "ShortValue" -> Short.class;
			case "ByteValue" -> Byte.class;
			default -> throw new IllegalStateException("Unknown ConfigValue type " + f.getType().getSimpleName());
		};
	}

	private void writeObject (Object obj, BufferedWriter w, int indent) throws IOException, IllegalAccessException {
		indent(w, indent);
		w.write("{");
		w.newLine();

		Field[] fields = obj.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field f = fields[i];
			f.setAccessible(true);

			String key = f.getName().toLowerCase();

			// 1) Comments
			Comment comment = f.getAnnotation(Comment.class);
			if (comment != null) {
				for (String line : comment.value()) {
					indent(w, indent + 1);
					w.write("// " + line);
					w.newLine();
				}
			}

			// 2) Nested @Config object?
			if (f.getType().isAnnotationPresent(Config.class)) {
				Object nested = f.get(obj);
				if (nested == null) {
					nested = instantiate(f.getType());
					f.set(obj, nested);
				}

				indent(w, indent + 1);
				w.write(key + ": ");
				writeObject(nested, w, indent + 1);
			}
			// 3) Any ConfigValue<?> (including ListValue<T>, EnumValue<E>, etc.)
			else if (ConfigValue.class.isAssignableFrom(f.getType())) {
				@SuppressWarnings("unchecked")
				ConfigValue<Object> cv = (ConfigValue<Object>) f.get(obj);
				Object val = cv.get();

				indent(w, indent + 1);
				w.write(key + ": ");

				if (val instanceof Collection<?> coll) {
					// ---- WRITE A JSON5 ARRAY ----
					w.write("[");
					if (!coll.isEmpty()) {
						w.newLine();
						Iterator<?> it = coll.iterator();
						while (it.hasNext()) {
							Object element = it.next();
							indent(w, indent + 2);

							if (element != null && element.getClass().isAnnotationPresent(Config.class)) {
								// element is a nested config object
								writeObject(element, w, indent + 2);
							} else {
								// primitive / enum / simple object
								String jsonElem = mapper.writeValueAsString(element);
								w.write(jsonElem);
							}

							if (it.hasNext()) w.write(",");
							w.newLine();
						}
						indent(w, indent + 1);
					}
					w.write("]");
				} else {
					// ---- WRITE A PRIMITIVE / STRING / ENUM ----
					String json = mapper.writeValueAsString(val);
					w.write(json);
				}
			}
			// 4) Skip any other fields
			else {
				continue;
			}

			//Comma between fields
			if (i < fields.length - 1) w.write(",");
			w.newLine();
		}

		indent(w, indent);
		w.write("}");
		if (indent == 0) w.newLine();
	}

	/**
	 * helper to indent
	 */
	private void indent (BufferedWriter w, int levels) throws IOException {
		for (int i = 0; i < levels; i++) w.write("    ");
	}

	/**
	 * Given a subclass of AbstractValue<X> (e.g. IntegerValue),
	 * pull out that X (Integer.class).
	 */
	private Class<?> unwrapValueType (Class<?> wrapper) {
		Type sup = wrapper.getGenericSuperclass();
		if (sup instanceof ParameterizedType pt
				&& pt.getRawType() == AbstractValue.class) {
			Type t = pt.getActualTypeArguments()[0];
			if (t instanceof Class<?> c) return c;
		}
		throw new IllegalStateException("Cannot unwrap wrapper type " + wrapper);
	}
}