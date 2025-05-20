package net.superscary.superconfig.manager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.annotations.Ignore;
import net.superscary.superconfig.format.ConfigFormat;
import net.superscary.superconfig.format.formats.JsonFormat;
import net.superscary.superconfig.value.AbstractValue;
import net.superscary.superconfig.value.ConfigValue;
import net.superscary.superconfig.value.wrappers.ListValue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the loading and saving of configuration objects.
 * <p>
 * This class provides functionality to load configuration objects from files and save them back
 * to disk. It supports various file formats through the {@link ConfigFormat} interface and
 * handles the serialization and deserialization of configuration objects.
 * </p>
 *
 * @param <T> the type of configuration object this manager handles
 * @author SuperScary
 * @see net.superscary.superconfig.format.ConfigFormat
 * @see net.superscary.superconfig.annotations.Config
 * @since 1.0.0
 */
public class ConfigManager<T> {
	/**
	 * The class type of the configuration object.
	 */
	private final Class<T> type;

	/**
	 * The file path where the configuration is stored.
	 */
	private final Path file;

	/**
	 * The format used for reading and writing the configuration.
	 */
	private final ConfigFormat format;

	/**
	 * The ObjectMapper used for serialization and deserialization.
	 */
	private final ObjectMapper mapper;

	/**
	 * Creates a new ConfigManager with the specified type, file, and format.
	 *
	 * @param type   the class type of the configuration object
	 * @param file   the file path where the configuration is stored
	 * @param fmt    the format to use for reading and writing
	 */
	public ConfigManager(Class<T> type, Path file, ConfigFormat fmt) {
		this.type = type;
		this.file = ensureExtension(file, fmt.extension());
		this.format = fmt;
		this.mapper = fmt.getMapper();
	}

	/**
	 * Creates a new builder for constructing a ConfigManager.
	 *
	 * @param type the class type of the configuration object
	 * @param <T>  the type of configuration object
	 * @return a new builder instance
	 */
	public static <T> Builder<T> builder(Class<T> type) {
		return new Builder<>(type);
	}

	/**
	 * Loads (or creates) the configuration instance.
	 * <p>
	 * If the configuration file exists, it will be loaded and merged with default values.
	 * If the file doesn't exist, a new instance will be created with default values.
	 * </p>
	 *
	 * @return the populated configuration instance
	 * @throws IOException            if an I/O error occurs
	 * @throws IllegalAccessException if the object is not accessible
	 */
	public T load() throws IOException, IllegalAccessException {
		T cfg = instantiate(type);
		if (Files.exists(file)) {
			JsonNode root = format.readTree(file);
			populate(cfg, root);
		}
		return cfg;
	}

	/**
	 * Saves the configuration object to disk.
	 * <p>
	 * The configuration will be written to the file specified in the constructor,
	 * using the format specified in the constructor.
	 * </p>
	 *
	 * @param config the configuration object to save
	 * @throws IOException if an I/O error occurs
	 */
	public void save(T config) throws IOException {
		format.write(file, config, type);
	}

	// ——— Internals ———

	/**
	 * Creates a new instance of the specified class.
	 *
	 * @param cls the class to instantiate
	 * @param <U> the type of the class
	 * @return a new instance of the class
	 * @throws IllegalStateException if instantiation fails
	 */
	private <U> U instantiate(Class<U> cls) {
		try {
			return cls.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to instantiate " + cls.getName(), e);
		}
	}

	/**
	 * Populates a configuration object with values from a JsonNode.
	 * <p>
	 * This method handles various types of fields:
	 * - Nested @Config objects
	 * - ListValue fields
	 * - Scalar ConfigValue fields
	 * </p>
	 *
	 * @param obj  the object to populate
	 * @param node the JsonNode containing the values
	 * @throws IOException            if an I/O error occurs
	 * @throws IllegalAccessException if the object is not accessible
	 */
	private void populate(Object obj, JsonNode node) throws IOException, IllegalAccessException {
		for (Field f : obj.getClass().getDeclaredFields()) {
			if (f.isAnnotationPresent(Ignore.class)) continue;

			f.setAccessible(true);
			String key = f.getName().toLowerCase();
			JsonNode child = node.get(key);

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

			if (ListValue.class.isAssignableFrom(f.getType())) {
				if (child == null || !child.isArray()) continue;

				Type gt = f.getGenericType();
				if (!(gt instanceof ParameterizedType pt)) {
					throw new IOException("Missing generic type for ListValue on field " + f.getName());
				}
				Type arg = pt.getActualTypeArguments()[0];
				if (!(arg instanceof Class<?> declaredElem)) {
					throw new IOException("Cannot handle generic type " + arg + " on field " + f.getName());
				}

				Class<?> elemType = declaredElem;
				if (ConfigValue.class.isAssignableFrom(declaredElem)) {
					elemType = unwrapValueType(declaredElem);
				}

				List<Object> built = new ArrayList<>();
				for (JsonNode elNode : child) {
					Object element;

					if (elemType.isAnnotationPresent(Config.class)) {
						element = instantiate(elemType);
						populate(element, elNode);
					} else {
						if (elNode.isValueNode()) {
							element = mapper.treeToValue(elNode, elemType);
						} else if (elNode.isObject()) {
							JsonNode valNode = elNode.get("value");
							if (valNode != null && valNode.isValueNode()) {
								element = mapper.treeToValue(valNode, elemType);
							} else {
								element = mapper.convertValue(elNode, elemType);
							}
						} else {
							element = mapper.convertValue(elNode, elemType);
						}
					}

					built.add(element);
				}

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

			if (ConfigValue.class.isAssignableFrom(f.getType())) {
				@SuppressWarnings("unchecked")
				ConfigValue<Object> cv = (ConfigValue<Object>) f.get(obj);
				if (child != null && !child.isNull()) {
					JsonNode valNode = child;
					if (valNode.isObject() && valNode.has("value")) {
						valNode = valNode.get("value");
					}

					Class<?> tgt = inferGenericType(f);
					Object v;

					if (tgt == Character.class && valNode.isTextual()) {
						String s = valNode.textValue();
						v = s.isEmpty() ? '\0' : s.charAt(0);
					}
					else if (valNode.isValueNode()) {
						v = mapper.treeToValue(valNode, tgt);
					}
					else {
						v = mapper.convertValue(valNode, tgt);
					}

					cv.set(v);
				}
			}
		}
	}

	/**
	 * Infers the generic type of a field.
	 * <p>
	 * This method is used to determine the target type for deserialization
	 * of ConfigValue fields.
	 * </p>
	 *
	 * @param f the field to infer the type for
	 * @return the inferred type
	 * @throws IllegalStateException if the type cannot be inferred
	 */
	private Class<?> inferGenericType(Field f) {
		Type gt = f.getGenericType();
		if (gt instanceof ParameterizedType pt) {
			Type[] args = pt.getActualTypeArguments();
			if (args.length == 1 && args[0] instanceof Class<?> argClass) {
				if (Enum.class.isAssignableFrom(argClass)) {
					@SuppressWarnings("unchecked")
					Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) argClass;
					return enumType;
				}
				return argClass;
			}
		}

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
	 * Unwraps a ConfigValue type to get its underlying value type.
	 * <p>
	 * For example, IntegerValue -> Integer.class
	 * </p>
	 *
	 * @param wrapper the wrapper type to unwrap
	 * @return the unwrapped type
	 * @throws IllegalStateException if the type cannot be unwrapped
	 */
	private Class<?> unwrapValueType(Class<?> wrapper) {
		Type sup = wrapper.getGenericSuperclass();
		if (sup instanceof ParameterizedType pt
				&& pt.getRawType() == AbstractValue.class) {
			Type t = pt.getActualTypeArguments()[0];
			if (t instanceof Class<?> c) return c;
		}
		throw new IllegalStateException("Cannot unwrap wrapper type " + wrapper);
	}

	/**
	 * Ensures a file path has the correct extension.
	 *
	 * @param file the file path to check
	 * @param ext  the extension to ensure
	 * @return the file path with the correct extension
	 */
	private static Path ensureExtension(Path file, String ext) {
		String name = file.getFileName().toString();
		if (!name.endsWith(ext)) {
			name = name + ext;
		}
		return (file.getParent() != null)
				? file.getParent().resolve(name)
				: Paths.get(name);
	}

	/**
	 * Merges two configuration objects.
	 * <p>
	 * Values from the override object take precedence over values in the base object.
	 * </p>
	 *
	 * @param base     the base configuration object
	 * @param override the override configuration object
	 * @throws IllegalAccessException if the objects are not accessible
	 */
	private void merge(T base, T override) throws IllegalAccessException {
		for (Field f : type.getDeclaredFields()) {
			f.setAccessible(true);
			Object val = f.get(override);
			if (val != null) {
				f.set(base, val);
			}
		}
	}

	/**
	 * Builder for constructing ConfigManager instances.
	 *
	 * @param <T> the type of configuration object
	 */
	public static final class Builder<T> {
		private final Class<T> type;
		private Path file;
		private ConfigFormat format = new JsonFormat();

		private Builder(Class<T> type) {
			this.type = type;
		}

		/**
		 * Sets the file path for the configuration.
		 *
		 * @param file the file path
		 * @return this builder
		 */
		public Builder<T> file(Path file) {
			this.file = file;
			return this;
		}

		/**
		 * Sets the format for the configuration.
		 *
		 * @param fmt the format to use
		 * @return this builder
		 */
		public Builder<T> format(ConfigFormat fmt) {
			this.format = fmt;
			return this;
		}

		/**
		 * Builds a new ConfigManager instance.
		 *
		 * @return a new ConfigManager
		 * @throws IllegalStateException if the file path is not set
		 */
		public ConfigManager<T> build() {
			if (file == null) throw new IllegalStateException("file not set");
			return new ConfigManager<>(type, file, format);
		}
	}
}