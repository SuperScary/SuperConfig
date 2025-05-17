package net.superscary.superconfig.format.formats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
 * other YAML-style extensions.
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
	 */
	private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).enable(SerializationFeature.INDENT_OUTPUT);

	/**
	 * Returns the file extension for this format.
	 */
	@Override
	public String extension () {
		return ".yml";
	}

	@Override
	public String lineCommentPrefix () {
		return "# ";
	}

	/**
	 * Returns the default YAML mapper. This is used to read and write JSON files.
	 */
	@Override
	public ObjectMapper getMapper () {
		return mapper;
	}

	/**
	 * Allows reading a YAML file into a Java object of the specified type.
	 */
	@Override
	public <T> T read (Path file, Class<T> type) throws IOException {
		return mapper.readValue(file.toFile(), type);
	}

	/**
	 * Allows writing a Java object to a YAML file.
	 */
	@Override
	public <T> void write (Path file, T config, Class<T> writer) throws IOException {
		try (BufferedWriter w = Files.newBufferedWriter(file,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writeYamlMapping(config, w, 0);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeYamlMapping (Object obj, BufferedWriter w, int indent)
			throws IOException, IllegalAccessException {
		for (Field f : obj.getClass().getDeclaredFields()) {
			int mods = f.getModifiers();
			// ← SKIP any static, transient or compiler‐generated fields
			if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || f.isSynthetic()) {
				continue;
			}

			f.setAccessible(true);
			Object raw = f.get(obj);

			// unwrap ConfigValue<T> if you use it
			if (raw instanceof ConfigValue<?> cv) {
				raw = cv.get();
			}

			String key = f.getName().toLowerCase();

			// 1) field-level comments
			Comment comment = f.getAnnotation(Comment.class);
			if (comment != null) {
				for (String line : comment.value()) {
					indent(w, indent);
					w.write(lineCommentPrefix() + line);
					w.newLine();
				}
			}

			// 2) null → "key: null"
			indent(w, indent);
			w.write(key + ":");
			if (raw == null) {
				w.write(" null");
				w.newLine();
				continue;
			}

			// 3) simple scalars
			if (isScalar(raw)) {
				w.write(" " + scalarToString(raw));
				w.newLine();
			}
			// 4) collections → sequence block
			else if (raw instanceof Collection<?> coll) {
				w.newLine();
				for (Object e : coll) {
					indent(w, indent + 2);
					w.write("- " + scalarToString(e));
					w.newLine();
				}
			}
			// 5) nested container → recurse
			else {
				w.newLine();
				writeYamlMapping(raw, w, indent + 2);
			}
		}
	}

	private boolean isScalar (Object v) {
		return v instanceof Number
				|| v instanceof Boolean
				|| v instanceof CharSequence
				|| v.getClass().isEnum();
	}

	private String scalarToString (Object v) {
		String s = v.toString();
		if (v instanceof CharSequence) {
			// wrap strings in quotes, escaping internal quotes
			s = "\"" + s.replace("\"", "\\\"") + "\"";
		}
		return s;
	}

	private void indent (BufferedWriter w, int levels) throws IOException {
		for (int i = 0; i < levels; i++) {
			w.write(" ");
		}
	}
}