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
 * other TOML-style extensions.
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
	 */
	private final ObjectMapper mapper;

	public TomlFormat () {
		this.mapper = new TomlMapper()
				.enable(SerializationFeature.INDENT_OUTPUT)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS,     true)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Returns the file extension for this format.
	 */
	@Override
	public String extension () {
		return ".toml";
	}

	@Override
	public String lineCommentPrefix () {
		return "# ";
	}

	/**
	 * Returns the default TOML mapper. This is used to read and write JSON files.
	 */
	@Override
	public ObjectMapper getMapper () {
		return mapper;
	}

	@Override
	public JsonNode readTree (Path file) throws IOException {
		return mapper.readTree(file.toFile());
	}

	/**
	 * Allows writing a Java object to a TOML file.
	 */
	@Override
	public <T> void write (Path file, T config, Class<T> writer) throws IOException {
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

		// ── First pass: handle simple values & arrays ──
		for (Field f : cls.getDeclaredFields()) {
			int mods = f.getModifiers();
			if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || f.isSynthetic()) {
				continue;
			}
			f.setAccessible(true);
			Object raw = f.get(obj);
			if (raw == null) continue;

			// unwrap ConfigValue<T> if you use it:
			if (raw instanceof ConfigValue<?> cv) {
				raw = cv.get();
				if (raw == null) continue;
			}

			boolean isCollection = raw instanceof Collection<?>;
			boolean isSimple = isSimple(raw);

			// defer nested containers to second pass
			if (!isSimple && !isCollection) {
				nestedFields.add(f);
				continue;
			}

			// emit this field’s comment
			emitFieldComment(f, w);

			String key = f.getName().toLowerCase();

			if (isCollection) {
				// e.g. tags = ["foo", "bar"]
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
				// simple scalar
				w.write(key + " = " + scalarToTomlString(raw));
				w.newLine();
			}
		}

		w.newLine();

		// ── Second pass: nested tables ──
		for (Field f : nestedFields) {
			int mods = f.getModifiers();
			if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || f.isSynthetic()) {
				continue;
			}
			f.setAccessible(true);
			Object nested = f.get(obj);
			if (nested == null) continue;

			// emit this section’s comment
			emitFieldComment(f, w);

			// write the table header
			String section = prefix.isEmpty()
					? f.getName().toLowerCase()
					: prefix + "." + f.getName().toLowerCase();
			w.write("[" + section + "]");
			w.newLine();

			// recurse into the nested object
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
				|| v.getClass().isEnum();
	}

	private String scalarToTomlString (Object v) {
		if (v instanceof CharSequence || v.getClass().isEnum()) {
			String s = v.toString()
					.replace("\\", "\\\\")
					.replace("\"", "\\\"");
			return "\"" + s + "\"";
		}
		return v.toString();
	}
}
