package net.superscary.superconfig.format.formats;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.*;
import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.format.ConfigFormat;
import net.superscary.superconfig.value.ConfigValue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Iterator;

/**
 * JSON5 config format using Jackson.
 * This format uses the Jackson library to read and write JSON files. It supports comments and
 * other JSON5-style extensions.
 *
 * @author SuperScary
 * @see net.superscary.superconfig.manager.ConfigManager
 * @see net.superscary.superconfig.annotations.Config
 * @see net.superscary.superconfig.annotations.Comment
 * @since 1.0.2
 */
public class Json5Format implements ConfigFormat {

	/**
	 * The default JSON5 mapper. This is used to read and write JSON files.
	 * The mapper is configured with the following settings:
	 * - Indented output for better readability
	 * - Case-insensitive property matching
	 * - Case-insensitive enum matching
	 * - Ignore unknown properties
	 */
	private final ObjectMapper mapper;

	/**
	 * Creates a new Json5Format instance.
	 * This constructor initializes the JSON5 mapper with the appropriate features
	 * and settings for JSON5-compliant JSON parsing and writing.
	 */
	public Json5Format() {
		JsonFactory factory = JsonFactory.builder()
				.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
				.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
				.enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
				.enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
				.enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
				.enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
				.build();

		this.mapper = new ObjectMapper(factory)
				.enable(SerializationFeature.INDENT_OUTPUT)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Returns the file extension for this format.
	 * The extension is ".json5" for JSON5 files.
	 *
	 * @return the file extension
	 */
	@Override
	public String extension() {
		return ".json5";
	}

	/**
	 * Returns the line comment prefix for this format.
	 * JSON5 comments use "// " as the prefix.
	 *
	 * @return the comment prefix
	 */
	@Override
	public String lineCommentPrefix() {
		return "// ";
	}

	/**
	 * Returns the default JSON5 mapper.
	 * This mapper is used to read and write JSON5 files with the configured settings.
	 *
	 * @return the ObjectMapper instance
	 */
	@Override
	public ObjectMapper getMapper() {
		return mapper;
	}

	/**
	 * Reads a JSON5 file and returns its contents as a JsonNode.
	 *
	 * @param file the file to read from
	 * @return the JsonNode representing the file contents
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public JsonNode readTree(Path file) throws IOException {
		return mapper.readTree(file.toFile());
	}

	/**
	 * Writes a configuration object to a JSON5 file.
	 * This method handles the serialization of the config object to JSON5 format,
	 * including proper indentation and formatting.
	 *
	 * @param file   the file to write to
	 * @param config the config object to write
	 * @param writer the class of the config object
	 * @param <T>    the type of the config object
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public <T> void write(Path file, T config, Class<T> writer) throws IOException {
		try (BufferedWriter w = Files.newBufferedWriter(file,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writeObject(config, w, 0);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private <U> U instantiate(Class<U> cls) {
		try {
			return cls.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to instantiate " + cls.getName(), e);
		}
	}

	private void writeObject(Object obj, BufferedWriter w, int indent) throws IOException, IllegalAccessException {
		indent(w, indent);
		w.write("{");
		w.newLine();

		Field[] fields = obj.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field f = fields[i];

			if (ignoreCheck(f)) continue;

			f.setAccessible(true);

			String key = f.getName().toLowerCase();

			Comment comment = f.getAnnotation(Comment.class);
			if (comment != null) {
				for (String line : comment.value()) {
					indent(w, indent + 1);
					w.write(this.lineCommentPrefix() + line + this.lineCommentSuffix());
					w.newLine();
				}
			}

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
			else if (ConfigValue.class.isAssignableFrom(f.getType())) {
				@SuppressWarnings("unchecked")
				ConfigValue<Object> cv = (ConfigValue<Object>) f.get(obj);
				Object val = cv.get();

				indent(w, indent + 1);
				w.write(key + ": ");

				if (val instanceof Collection<?> coll) {
					w.write("[");
					if (!coll.isEmpty()) {
						w.newLine();
						Iterator<?> it = coll.iterator();
						while (it.hasNext()) {
							Object element = it.next();
							indent(w, indent + 2);

							if (element != null && element.getClass().isAnnotationPresent(Config.class)) {
								writeObject(element, w, indent + 2);
							} else {
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
					String json = mapper.writeValueAsString(val);
					w.write(json);
				}
			}
			else {
				continue;
			}

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
	private void indent(BufferedWriter w, int levels) throws IOException {
		for (int i = 0; i < levels; i++) w.write("    ");
	}
}