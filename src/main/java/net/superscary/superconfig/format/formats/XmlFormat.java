package net.superscary.superconfig.format.formats;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
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
 * XML config format using Jackson.
 * <p>
 * This format uses the Jackson library to read and write XML files. It supports comments and
 * other XML-style extensions.
 * </p>
 *
 * @author SuperScary
 * @see net.superscary.superconfig.manager.ConfigManager
 * @see net.superscary.superconfig.annotations.Config
 * @see net.superscary.superconfig.annotations.Comment
 * @since 1.0.2
 */
public class XmlFormat implements ConfigFormat {

	/**
	 * The default XML mapper. This is used to read and write XML files.
	 */
	private final ObjectMapper mapper;

	public XmlFormat () {
		this.mapper = new XmlMapper()
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
		return ".xml";
	}

	@Override
	public String lineCommentPrefix () {
		return "<!-- ";
	}

	@Override
	public String lineCommentSuffix () {
		return " -->";
	}

	/**
	 * Returns the default XML mapper. This is used to read and write JSON files.
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
	 * Allows writing a Java object to an XML file.
	 */
	@Override
	public <T> void write (Path file, T config, Class<T> writer) throws IOException {
		try (BufferedWriter w = Files.newBufferedWriter(file,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			// XML prologue
			w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			w.newLine();

			// root element named after the class
			writeXmlElement(config, w, 0, writer.getSimpleName().toLowerCase());
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeXmlElement(Object obj, BufferedWriter w, int indent, String elementName)
			throws IOException, IllegalAccessException {
		// 1) Open this element
		indent(w, indent);
		w.write("<" + elementName + ">");
		w.newLine();

		// 2) Iterate only your config’s instance fields
		for (Field f : obj.getClass().getDeclaredFields()) {

			if (ignoreCheck(f)) continue;

			int mods = f.getModifiers();
			if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || f.isSynthetic()) {
				continue;
			}

			f.setAccessible(true);
			Object raw = f.get(obj);

			// 3) Unwrap any ConfigValue<?> wrapper
			if (raw instanceof ConfigValue<?> cv) {
				raw = cv.get();
			}

			// 4) Emit @Comment if present
			Comment comment = f.getAnnotation(Comment.class);
			if (comment != null && raw != null) {
				for (String line : comment.value()) {
					indent(w, indent + 1);
					w.write("<!-- " + line + " -->");
					w.newLine();
				}
			}

			String key = f.getName().toLowerCase();

			// 5) Nested container? recurse and let it write its own open+close
			if (raw != null && raw.getClass().isAnnotationPresent(Config.class)) {
				writeXmlElement(raw, w, indent + 1, key);
				continue;
			}

			// 6) Null => empty element
			if (raw == null) {
				indent(w, indent + 1);
				w.write("<" + key + "/>");
				w.newLine();
				continue;
			}

			// 7) Collection => <key>…</key> with <item> children
			if (raw instanceof Collection<?> coll) {
				indent(w, indent + 1);
				w.write("<" + key + ">");
				w.newLine();
				for (Object e : coll) {
					indent(w, indent + 2);
					w.write("<item>" + escapeXml(e.toString()) + "</item>");
					w.newLine();
				}
				indent(w, indent + 1);
				w.write("</" + key + ">");
				w.newLine();
				continue;
			}

			// 8) Scalar or enum or Class<?> => simple text element
			indent(w, indent + 1);
			w.write("<" + key + ">" + escapeXml(raw.toString()) + "</" + key + ">");
			w.newLine();
		}

		// 9) Close this element
		indent(w, indent);
		w.write("</" + elementName + ">");
		w.newLine();
	}


	private boolean isPrimitiveOrString (Object v) {
		return v instanceof Number
				|| v instanceof Boolean
				|| v instanceof CharSequence
				|| v.getClass().isEnum();
	}

	private String escapeXml (Object v) {
		String s = v.toString();
		return s
				.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}

	private void indent (BufferedWriter w, int levels) throws IOException {
		for (int i = 0; i < levels; i++) {
			w.write("    ");
		}
	}

}
