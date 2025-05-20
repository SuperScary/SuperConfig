package net.superscary.superconfig.format.formats;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TOML config format using Jackson.
 * <p>
 * This format uses the Jackson library to read and write TOML files. It supports comments and
 * other TOML-style extensions. The format is configured to be case-insensitive for properties
 * and enums, and to ignore unknown properties.
 * </p>
 *
 * @author SuperScary
 * @see net.superscary.superconfig.manager.ConfigManager
 * @see net.superscary.superconfig.annotations.Config
 * @see net.superscary.superconfig.annotations.Comment
 * @since 1.0.2
 */
public class TomlFormat implements ConfigFormat {

	/**
	 * The default TOML mapper. This is used to read and write TOML files.
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
	 * Creates a new TomlFormat with default settings.
	 * <p>
	 * The ObjectMapper is configured with standard settings for TOML processing,
	 * including support for comments and other TOML features.
	 * </p>
	 */
	public TomlFormat() {
		this.mapper = new TomlMapper()
				.enable(SerializationFeature.INDENT_OUTPUT)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Returns the file extension for this format.
	 * <p>
	 * The extension is ".toml" for TOML files.
	 * </p>
	 *
	 * @return the file extension
	 */
	@Override
	public String extension() {
		return ".toml";
	}

	/**
	 * Returns the line comment prefix for this format.
	 * <p>
	 * TOML comments use "# " as the prefix.
	 * </p>
	 *
	 * @return the comment prefix
	 */
	@Override
	public String lineCommentPrefix() {
		return "# ";
	}

	/**
	 * Returns the default TOML mapper.
	 * <p>
	 * This mapper is used to read and write TOML files with the configured settings.
	 * </p>
	 *
	 * @return the ObjectMapper instance
	 */
	@Override
	public ObjectMapper getMapper() {
		return mapper;
	}

	/**
	 * Reads a TOML file and returns its contents as a JsonNode.
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
	 * Writes a configuration object to a TOML file.
	 * <p>
	 * This method handles the serialization of the config object to TOML format,
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
			writeTomlMapping(config, w, "");
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeTomlMapping (Object obj, BufferedWriter w, String prefix)
			throws IOException, IllegalAccessException {
		Class<?> cls = obj.getClass();
		List<Field> nestedFields = new ArrayList<>();

		for (Field f : cls.getDeclaredFields()) {

			if (ignoreCheck(f)) continue;

			int mods = f.getModifiers();
			if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || f.isSynthetic()) {
				continue;
			}
			f.setAccessible(true);
			Object raw = f.get(obj);
			if (raw == null) continue;

			if (raw instanceof ConfigValue<?> cv) {
				raw = cv.get();
				if (raw == null) continue;
			}

			boolean isCollection = raw instanceof Collection<?>;
			boolean isSimple = isSimple(raw);

			if (!isSimple && !isCollection) {
				nestedFields.add(f);
				continue;
			}

			emitFieldComment(f, w);

			String key = f.getName().toLowerCase();

			if (isCollection) {
				@SuppressWarnings("unchecked")
				Collection<Object> coll = (Collection<Object>) raw;
				w.write(key + " = [");
				boolean first = true;
				for (Object e : coll) {
					if (!first) w.write(", ");
					w.write(scalarToTomlString(e));
					first = false;
				}
				w.write("]");
				w.newLine();
			} else {
				w.write(key + " = " + scalarToTomlString(raw));
				w.newLine();
			}
		}

		w.newLine();

		for (Field f : nestedFields) {
			int mods = f.getModifiers();
			if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || f.isSynthetic()) {
				continue;
			}
			f.setAccessible(true);
			Object nested = f.get(obj);
			if (nested == null) continue;

			emitFieldComment(f, w);

			String section = prefix.isEmpty()
					? f.getName().toLowerCase()
					: prefix + "." + f.getName().toLowerCase();
			w.write("[" + section + "]");
			w.newLine();

			writeTomlMapping(nested, w, section);

			w.newLine();
		}
	}

	private void emitFieldComment (Field f, BufferedWriter w) throws IOException {
		Comment c = f.getAnnotation(Comment.class);
		if (c != null) {
			for (String line : c.value()) {
				w.write(lineCommentPrefix() + line);
				w.newLine();
			}
		}
	}

	private boolean isSimple (Object v) {
		return v instanceof Number
				|| v instanceof Boolean
				|| v instanceof CharSequence
				|| v instanceof Character
				|| v.getClass().isEnum();
	}

	private String scalarToTomlString (Object v) {
		if (v instanceof CharSequence || v.getClass().isEnum()) {
			String s = v.toString()
					.replace("\\", "\\\\")
					.replace("\"", "\\\"");
			return "\"" + s + "\"";
		} else if (v instanceof Character) {
			String s = v.toString()
					.replace("\\", "\\\\")
					.replace("\"", "\\\"");
			return "'" + s + "'";
		}
		return v.toString();
	}
}
