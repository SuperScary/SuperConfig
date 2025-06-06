package net.superscary.superconfig.format.formats;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.format.ConfigFormat;
import net.superscary.superconfig.value.ConfigValue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

/**
 * YAML config format using Jackson.
 * <p>
 * This format uses the Jackson library to read and write YAML files. It supports comments and
 * other YAML-style extensions. The format is configured to be case-insensitive for properties
 * and enums, and to ignore unknown properties.
 * </p>
 *
 * @author SuperScary
 * @see net.superscary.superconfig.manager.ConfigManager
 * @see net.superscary.superconfig.annotations.Config
 * @see net.superscary.superconfig.annotations.Comment
 * @since 1.0.2
 */
public class YamlFormat implements ConfigFormat {
	/**
	 * The default YAML mapper. This is used to read and write YAML files.
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
	 * Creates a new YamlFormat with default settings.
	 * <p>
	 * The ObjectMapper is configured with standard settings for YAML processing,
	 * including support for comments and other YAML features.
	 * </p>
	 */
	public YamlFormat() {
		this.mapper = new ObjectMapper(new YAMLFactory())
				.enable(SerializationFeature.INDENT_OUTPUT)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Returns the file extension for this format.
	 * <p>
	 * The extension is ".yml" for YAML files.
	 * </p>
	 *
	 * @return the file extension
	 */
	@Override
	public String extension() {
		return ".yml";
	}

	/**
	 * Returns the line comment prefix for this format.
	 * <p>
	 * YAML comments use "# " as the prefix.
	 * </p>
	 *
	 * @return the comment prefix
	 */
	@Override
	public String lineCommentPrefix() {
		return "# ";
	}

	/**
	 * Returns the default YAML mapper.
	 * <p>
	 * This mapper is used to read and write YAML files with the configured settings.
	 * </p>
	 *
	 * @return the ObjectMapper instance
	 */
	@Override
	public ObjectMapper getMapper() {
		return mapper;
	}

	/**
	 * Reads a YAML file and returns its contents as a JsonNode.
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
	 * Writes a configuration object to a YAML file.
	 * <p>
	 * This method handles the serialization of the config object to YAML format,
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
			writeYamlMapping(config, w, 0);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeYamlMapping(Object obj, BufferedWriter w, int indent)
			throws IOException, IllegalAccessException {
		for (Field f : obj.getClass().getDeclaredFields()) {

			if (ignoreCheck(f)) continue;

			int mods = f.getModifiers();
			if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || f.isSynthetic()) {
				continue;
			}

			f.setAccessible(true);
			Object raw = f.get(obj);

			if (raw instanceof ConfigValue<?> cv) {
				raw = cv.get();
			}

			String key = f.getName().toLowerCase();

			Comment comment = f.getAnnotation(Comment.class);
			if (comment != null) {
				for (String line : comment.value()) {
					indent(w, indent);
					w.write(lineCommentPrefix() + line);
					w.newLine();
				}
			}

			indent(w, indent);
			w.write(key + ":");
			if (raw == null) {
				w.write(" null");
				w.newLine();
				continue;
			}

			if (isScalar(raw)) {
				w.write(" " + scalarToString(raw));
				w.newLine();
			}
			else if (raw instanceof Collection<?> coll) {
				w.newLine();
				for (Object e : coll) {
					indent(w, indent + 2);
					w.write("- " + scalarToString(e));
					w.newLine();
				}
			}
			else {
				w.newLine();
				writeYamlMapping(raw, w, indent + 2);
			}
		}
	}

	private boolean isScalar(Object v) {
		return v instanceof Number
				|| v instanceof Boolean
				|| v instanceof CharSequence
				|| v instanceof Character
				|| v.getClass().isEnum();
	}

	private String scalarToString(Object v) {
		String s = v.toString();
		if (v instanceof CharSequence) {
			s = "\"" + s.replace("\"", "\\\"") + "\"";
		} else if (v instanceof Character) {
			s = "'" + s.replace("'", "\\'") + "'";
		}
		return s;
	}

	private void indent(BufferedWriter w, int levels) throws IOException {
		for (int i = 0; i < levels; i++) {
			w.write(" ");
		}
	}
}