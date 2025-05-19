package net.superscary.superconfig.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.annotations.Ignore;
import net.superscary.superconfig.format.ConfigFormat;
import net.superscary.superconfig.format.formats.JsonFormat;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ConfigManager<T> {
	private final Class<T> type;
	private final Path file;
	private final ConfigFormat format;
	private final ObjectMapper mapper;

	public ConfigManager (Class<T> type, Path file, ConfigFormat fmt) {
		this.type = type;
		this.file = ensureExtension(file, fmt.extension());
		this.format = fmt;
		this.mapper = fmt.getMapper();
	}

	public static <T> Builder<T> builder (Class<T> type) {
		return new Builder<>(type);
	}

	/**
	 * Load (or create) the config instance
	 */
	public T load () throws IOException, IllegalAccessException {
		T cfg = instantiate(type);
		if (Files.exists(file)) {
			JsonNode root = format.readTree(file);
			populate(cfg, root);    // your original method that handles ListValue, ConfigValue, nested @Config, etc.
		}
		return cfg;
	}

	/**
	 * Write out with comments
	 */
	public void save (T config) throws IOException {
		/*try (BufferedWriter w = Files.newBufferedWriter(file,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING)) {
			writeObject(config, w, 0);
		}*/
		format.write(file, config, type);
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
			if (f.isAnnotationPresent(Ignore.class)) continue;

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
						} else if (elNode.isObject()) {
							// maybe old‐style wrapper object? try to pull "value" field
							JsonNode valNode = elNode.get("value");
							if (valNode != null && valNode.isValueNode()) {
								element = mapper.treeToValue(valNode, elemType);
							} else {
								// last resort: try binding the whole object to elemType
								element = mapper.convertValue(elNode, elemType);
							}
						} else {
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
					// 1) unwrap any { "value": ... } wrapper
					JsonNode valNode = child;
					if (valNode.isObject() && valNode.has("value")) {
						valNode = valNode.get("value");
					}

					// 2) figure out the target type (e.g. Character.class)
					Class<?> tgt = inferGenericType(f);
					Object v;

					// 3) special case char from a String node
					if (tgt == Character.class && valNode.isTextual()) {
						String s = valNode.textValue();
						v = s.isEmpty() ? '\0' : s.charAt(0);
					}
					// 4) any other simple value node
					else if (valNode.isValueNode()) {
						v = mapper.treeToValue(valNode, tgt);
					}
					// 5) fallback for weird cases
					else {
						v = mapper.convertValue(valNode, tgt);
					}

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
			case "CharValue" -> Character.class;
			default -> throw new IllegalStateException("Unknown ConfigValue type " + f.getType().getSimpleName());
		};
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

	private static Path ensureExtension (Path file, String ext) {
		String name = file.getFileName().toString();
		if (!name.endsWith(ext)) {
			name = name + ext;
		}
		return (file.getParent() != null)
				? file.getParent().resolve(name)
				: Paths.get(name);
	}

	private void merge (T base, T override) throws IllegalAccessException {
		for (Field f : type.getDeclaredFields()) {
			f.setAccessible(true);
			Object val = f.get(override);
			if (val != null) {         // or more nuanced checks if you need primitives
				f.set(base, val);
			}
		}
	}

	public static final class Builder<T> {
		private final Class<T> type;
		private Path file;
		private ConfigFormat format = new JsonFormat();

		private Builder (Class<T> type) {
			this.type = type;
		}

		public Builder<T> file (Path file) {
			this.file = file;
			return this;
		}

		public Builder<T> format (ConfigFormat fmt) {
			this.format = fmt;
			return this;
		}

		public ConfigManager<T> build () {
			if (file == null) throw new IllegalStateException("file not set");
			return new ConfigManager<>(type, file, format);
		}
	}
}