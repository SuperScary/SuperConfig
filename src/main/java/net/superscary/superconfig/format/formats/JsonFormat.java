package net.superscary.superconfig.format.formats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.format.ConfigFormat;
import net.superscary.superconfig.writer.ConfigWriter;

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
 * other JSON5-style extensions.
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
	 */
	private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	/**
	 * Returns the file extension for this format.
	 */
	@Override
	public String extension () {
		return ".json";
	}

	@Override
	public String lineCommentPrefix () {
		return "// ";
	}

	/**
	 * Returns the default JSON mapper. This is used to read and write JSON files.
	 */
	@Override
	public ObjectMapper getMapper () {
		return mapper;
	}

	/**
	 * Allows reading a JSON file into a Java object of the specified type.
	 */
	@Override
	public <T> T read (Path file, Class<T> type) throws IOException {
		return mapper.readValue(file.toFile(), type);
	}

	/**
	 * Allows writing a Java object to a JSON file.
	 */
	@Override
	public <T> void write (Path file, T config, Class<T> writer) throws IOException {
		try (BufferedWriter w = Files.newBufferedWriter(file,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writeJsonObject(config, w, 0);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeJsonObject (Object obj, BufferedWriter w, int indent) throws IOException, IllegalAccessException {
		indent(w, indent);
		w.write("{");
		w.newLine();

		Field[] fields = obj.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field f = fields[i];
			f.setAccessible(true);

			// 1) Emit @Comment lines
			// Standard JSON does not support comments, but JSON5 does.
			/*Comment comment = f.getAnnotation(Comment.class);
			if (comment != null) {
				for (String line : comment.value()) {
					indent(w, indent + 1);
					w.write(lineCommentPrefix() + line);
					w.newLine();
				}
			}*/

			String key = f.getName();
			Object val = f.get(obj);

			// 2) Key
			indent(w, indent + 1);
			w.write("\"" + key + "\": ");

			// 3) Value
			if (val == null) {
				w.write("null");
			}
			// 3a) Nested @Config container
			else if (f.getType().isAnnotationPresent(Config.class)) {
				writeJsonObject(val, w, indent + 1);
			}
			// 3b) Collections → JSON array
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
			// 3c) Primitive / String / Enum
			else {
				String json = mapper.writeValueAsString(val);
				w.write(json);
			}

			// 4) Comma if not last
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

	private void indent (BufferedWriter w, int levels) throws IOException {
		for (int i = 0; i < levels; i++) {
			w.write("    ");
		}
	}

}