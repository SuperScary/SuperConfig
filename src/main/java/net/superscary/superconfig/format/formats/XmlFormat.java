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
 * This format uses the Jackson library to read and write XML files. It supports comments and
 * other XML-style extensions. The format is configured to be case-insensitive for properties
 * and enums, and to ignore unknown properties.
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
	 * The mapper is configured with the following settings:
	 * - Indented output for better readability
	 * - Case-insensitive property matching
	 * - Case-insensitive enum matching
	 * - Ignore unknown properties
	 */
	private final ObjectMapper mapper;

	/**
	 * Creates a new XmlFormat with default settings.
	 * The ObjectMapper is configured with standard settings for XML processing,
	 * including support for comments and other XML features.
	 */
	public XmlFormat() {
		this.mapper = new XmlMapper()
				.enable(SerializationFeature.INDENT_OUTPUT)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
				.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	/**
	 * Returns the file extension for this format.
	 * The extension is ".xml" for XML files.
	 *
	 * @return the file extension
	 */
	@Override
	public String extension() {
		return ".xml";
	}

	/**
	 * Returns the line comment prefix for this format.
	 *
	 * @return the comment prefix
	 */
	@Override
	public String lineCommentPrefix() {
		return "<!-- ";
	}

	/**
	 * Returns the line comment suffix for this format.
	 * XML comments use " -->" as the suffix.
	 *
	 * @return the comment suffix
	 */
	@Override
	public String lineCommentSuffix() {
		return " -->";
	}

	/**
	 * Returns the default XML mapper.
	 * This mapper is used to read and write XML files with the configured settings.
	 *
	 * @return the ObjectMapper instance
	 */
	@Override
	public ObjectMapper getMapper() {
		return mapper;
	}

	/**
	 * Reads an XML file and returns its contents as a JsonNode.
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
	 * Writes a configuration object to an XML file.
	 * This method handles the serialization of the config object to XML format,
	 * including proper indentation, formatting, and XML prologue.
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
			// XML prologue
			w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			w.newLine();

			writeXmlElement(config, w, 0, writer.getSimpleName().toLowerCase());
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeXmlElement(Object obj, BufferedWriter w, int indent, String elementName)
			throws IOException, IllegalAccessException {
		indent(w, indent);
		w.write("<" + elementName + ">");
		w.newLine();

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

			Comment comment = f.getAnnotation(Comment.class);
			if (comment != null && raw != null) {
				for (String line : comment.value()) {
					indent(w, indent + 1);
					w.write("<!-- " + line + " -->");
					w.newLine();
				}
			}

			String key = f.getName().toLowerCase();

			if (raw != null && raw.getClass().isAnnotationPresent(Config.class)) {
				writeXmlElement(raw, w, indent + 1, key);
				continue;
			}

			if (raw == null) {
				indent(w, indent + 1);
				w.write("<" + key + "/>");
				w.newLine();
				continue;
			}

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
