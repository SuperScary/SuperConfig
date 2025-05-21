package net.superscary.superconfig.format.formats;

import net.superscary.superconfig.core.ConfigSerializer;
import net.superscary.superconfig.format.AbstractConfigFormatAdapter;
import net.superscary.superconfig.format.ConfigFormatType;
import net.superscary.superconfig.format.features.XmlFeatures;
import net.superscary.superconfig.format.tokenizer.XmlTokenizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Format adapter for XML configuration files.
 * <p>
 * This adapter handles reading and writing configuration data in XML format,
 * with support for comments, attributes, and nested elements.
 * </p>
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class XmlFormatAdapter extends AbstractConfigFormatAdapter {
	private final XmlFeatures features;
	private final ConfigSerializer serializer;

	public XmlFormatAdapter () {
		this(XmlFeatures.builder().build());
	}

	public XmlFormatAdapter (XmlFeatures features) {
		this.features = features;
		this.serializer = new ConfigSerializer();
	}

	@Override
	public String extension () {
		return ConfigFormatType.XML.getFileExtension();
	}

	@Override
	public String lineCommentPrefix () {
		return "<!--";
	}

	@Override
	public String lineCommentSuffix () {
		return "-->";
	}

	@Override
	public Map<String, Object> read (Path file) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file)) {
			return parse(reader);
		}
	}

	@Override
	protected Map<String, Object> parse (BufferedReader reader) throws IOException {
		XmlTokenizer tokenizer = new XmlTokenizer(reader, features);
		return tokenizer.tokenize();
	}

	@Override
	public <T> void write (Path file, T config, Class<T> cls) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(file)) {
			writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			writeObject(config, writer, 0);
		} catch (IllegalAccessException e) {
			throw new IOException("Failed to serialize " + cls.getName(), e);
		}
	}

	@Override
	protected void writeObject (Object obj, BufferedWriter writer, int indent) throws IOException, IllegalAccessException {
		Map<String, Object> data = serializer.toMap(obj);
		formatNode(data, writer, indent);
	}

	@Override
	protected void indent (BufferedWriter writer, int levels) throws IOException {
		writer.write("  ".repeat(levels));
	}

	private void formatNode (Map<String, Object> data, BufferedWriter writer, int indent) throws IOException {
		// Collect comments first
		Map<String, List<String>> comments = new java.util.HashMap<>();
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			String key = entry.getKey();
			if (key.startsWith("__class_comments")) {
				comments.put(key, (List<String>) entry.getValue());
			} else if (key.startsWith("__field_comments_")) {
				comments.put(key, (List<String>) entry.getValue());
			}
		}

		// Write class-level comments
		List<String> classComments = comments.get("__class_comments");
		if (classComments != null) {
			for (String comment : classComments) {
				indent(writer, indent);
				writer.write(lineCommentPrefix() + " " + comment + " " + lineCommentSuffix());
				writer.newLine();
			}
		}

		// Process the actual data
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			// Skip comment entries
			if (key.startsWith("__")) {
				continue;
			}

			// Write field comments if they exist
			List<String> fieldComments = comments.get("__field_comments_" + key);
			if (fieldComments != null) {
				for (String comment : fieldComments) {
					indent(writer, indent);
					writer.write(lineCommentPrefix() + " " + comment + " " + lineCommentSuffix());
					writer.newLine();
				}
			}

			// Write the element
			indent(writer, indent);
			writer.write("<" + key);

			if (value instanceof Map) {
				writer.write(">");
				writer.newLine();
				formatNode((Map<String, Object>) value, writer, indent + 1);
				indent(writer, indent);
				writer.write("</" + key + ">");
			} else {
				writer.write(">" + formatValue(value) + "</" + key + ">");
			}
			writer.newLine();
		}
	}

	private String formatValue (Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof String) {
			return escapeXml((String) value);
		}
		if (value instanceof Number || value instanceof Boolean) {
			return value.toString();
		}
		if (value instanceof List) {
			StringBuilder sb = new StringBuilder();
			for (Object item : (List<?>) value) {
				if (!sb.isEmpty()) {
					sb.append(", ");
				}
				sb.append(formatValue(item));
			}
			return sb.toString();
		}
		return value.toString();
	}

	private String escapeXml (String value) {
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}
} 