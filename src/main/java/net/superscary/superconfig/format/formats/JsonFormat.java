package net.superscary.superconfig.format.formats;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.*;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.format.ConfigFormat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Iterator;

/**
 * JSON config format using Jackson.
 * <p>
 * This format uses the Jackson library to read and write JSON files. It supports comments and
 * other JSON5-style extensions. The format is configured to be case-insensitive for properties
 * and enums, and to ignore unknown properties.
 * </p>
 *
 * @author SuperScary
 * @see net.superscary.superconfig.manager.ConfigManager
 * @see net.superscary.superconfig.annotations.Config
 * @see net.superscary.superconfig.annotations.Comment
 * @since 1.0.2
 */
public class JsonFormat implements ConfigFormat {

	/**
	 * The default JSON mapper. This is used to read and write JSON files.
	 * <p>
	 * The mapper is configured with the following settings:
	 * - Indented output for better readability
	 * - Case-insensitive property matching
	 * - Case-insensitive enum matching
	 * - Ignore unknown properties
	 * </p>
	 */
	private final ObjectMapper mapper;

	/**
	 * Creates a new JsonFormat with default settings.
	 * <p>
	 * The ObjectMapper is configured with standard settings for JSON processing,
	 * including support for comments and other JSON5 features.
	 * </p>
	 */
	public JsonFormat() {
		JsonFactory factory = JsonFactory.builder().build();

		this.mapper = new ObjectMapper(factory)
				.enable(SerializationFeature.INDENT_OUTPUT)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Returns the file extension for this format.
	 * <p>
	 * The extension is ".json" for JSON files.
	 * </p>
	 *
	 * @return the file extension
	 */
	@Override
	public String extension() {
		return ".json";
	}

	/**
	 * Returns the line comment prefix for this format.
	 * <p>
	 * JSON comments use "// " as the prefix.
	 * </p>
	 *
	 * @return the comment prefix
	 */
	@Override
	public String lineCommentPrefix() {
		return "// ";
	}

	/**
	 * Returns the default JSON mapper.
	 * <p>
	 * This mapper is used to read and write JSON files with the configured settings.
	 * </p>
	 *
	 * @return the ObjectMapper instance
	 */
	@Override
	public ObjectMapper getMapper() {
		return mapper;
	}

	/**
	 * Reads a JSON file and returns its contents as a JsonNode.
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
	 * Writes a configuration object to a JSON file.
	 * <p>
	 * This method handles the serialization of the config object to JSON format,
	 * including proper indentation and formatting.
	 * </p>
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
			writeJsonObject(config, w, 0);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeJsonObject(Object obj, BufferedWriter w, int indent) throws IOException, IllegalAccessException {
		indent(w, indent);
		w.write("{");
		w.newLine();

		Field[] fields = obj.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field f = fields[i];

			if (ignoreCheck(f)) continue;

			f.setAccessible(true);

			String key = f.getName();
			Object val = f.get(obj);

			indent(w, indent + 1);
			w.write("\"" + key + "\": ");

			if (val == null) {
				w.write("null");
			}
			else if (f.getType().isAnnotationPresent(Config.class)) {
				writeJsonObject(val, w, indent + 1);
			}
			else if (val instanceof Collection<?> coll) {
				w.write("[");
				if (!coll.isEmpty()) {
					w.newLine();
					Iterator<?> it = coll.iterator();
					while (it.hasNext()) {
						Object element = it.next();
						indent(w, indent + 2);
						if (element != null && element.getClass().isAnnotationPresent(Config.class)) {
							writeJsonObject(element, w, indent + 2);
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
			}
			else {
				String json = mapper.writeValueAsString(val);
				w.write(json);
			}

			if (i < fields.length - 1) {
				w.write(",");
			}
			w.newLine();
		}

		indent(w, indent);
		w.write("}");
		if (indent == 0) {
			w.newLine();
		}
	}

	private void indent(BufferedWriter w, int levels) throws IOException {
		for (int i = 0; i < levels; i++) {
			w.write("    ");
		}
	}

}