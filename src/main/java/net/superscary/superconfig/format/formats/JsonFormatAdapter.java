package net.superscary.superconfig.format.formats;

import net.superscary.superconfig.core.ConfigSerializer;
import net.superscary.superconfig.format.AbstractConfigFormatAdapter;
import net.superscary.superconfig.format.ConfigFormatType;
import net.superscary.superconfig.format.features.JsonFeatures;
import net.superscary.superconfig.format.tokenizer.JsonTokenizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Standard JSON format adapter.
 * This adapter implements the standard JSON format without comments or other JSON5 extensions.
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class JsonFormatAdapter extends AbstractConfigFormatAdapter {
	private JsonTokenizer tokenizer;
	private final ConfigSerializer serializer;
	private final JsonFeatures features;

	/**
	 * Creates a new JsonFormatAdapter with standard JSON features.
	 * Standard JSON does not support comments, trailing commas, or other JSON5 extensions.
	 */
	public JsonFormatAdapter () {
		this.features = JsonFeatures.getInstance();
		this.tokenizer = new JsonTokenizer(features);
		this.serializer = new ConfigSerializer();
	}

	/**
	 * Sets the known fields for the tokenizer to validate against.
	 * This is used to detect unmapped fields in the configuration file.
	 *
	 * @param knownFields the set of known field names
	 */
	public void setKnownFields (Set<String> knownFields) {
		this.tokenizer = new JsonTokenizer(features, knownFields);
	}

	@Override
	public String extension () {
		return ConfigFormatType.JSON.getExtension();
	}

	@Override
	public String lineCommentPrefix () {
		return ""; // Standard JSON doesn't support comments
	}

	@Override
	public Map<String, Object> read (Path file) throws IOException {
		String content = Files.readString(file);
		return tokenizer.parse(content);
	}

	@Override
	protected Map<String, Object> parse (BufferedReader reader) throws IOException {
		StringBuilder content = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			content.append(line).append("\n");
		}
		return tokenizer.parse(content.toString());
	}

	@Override
	public <T> void write (Path file, T config, Class<T> cls) throws IOException {
		try {
			Map<String, Object> data = serializer.toMap(config);
			String json = formatJson(data);
			Files.writeString(file, json);
		} catch (IllegalAccessException e) {
			throw new IOException("Failed to serialize " + cls.getName(), e);
		}
	}

	@Override
	protected void writeObject (Object obj, BufferedWriter writer, int indent) throws IOException {
		if (obj instanceof Map<?, ?> map) {
			writer.write("{\n");
			boolean first = true;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (!first) {
					writer.write(",\n");
				}
				first = false;
				indent(writer, indent + 1);
				writer.write("\"" + entry.getKey() + "\": ");
				writeValue(entry.getValue(), writer, indent + 1);
			}
			writer.write("\n");
			indent(writer, indent);
			writer.write("}");
		} else {
			writeValue(obj, writer, indent);
		}
	}

	private void writeValue (Object value, BufferedWriter writer, int indent) throws IOException {
		if (value == null) {
			writer.write("null");
		} else if (value instanceof String) {
			writer.write("\"" + escapeString((String) value) + "\"");
		} else if (value instanceof Number || value instanceof Boolean) {
			writer.write(value.toString());
		} else if (value instanceof Iterable<?>) {
			writer.write("[\n");
			boolean first = true;
			for (Object item : (Iterable<?>) value) {
				if (!first) {
					writer.write(",\n");
				}
				first = false;
				indent(writer, indent + 1);
				writeValue(item, writer, indent + 1);
			}
			writer.write("\n");
			indent(writer, indent);
			writer.write("]");
		} else if (value instanceof Map<?, ?>) {
			writeObject(value, writer, indent);
		} else {
			writer.write("\"" + value + "\"");
		}
	}

	@Override
	protected void indent (BufferedWriter writer, int indent) throws IOException {
		writer.write("    ".repeat(indent));
	}

	/**
	 * Formats a Map into a standard JSON string.
	 * This method ensures the output follows strict JSON format without any JSON5 extensions.
	 *
	 * @param data the data to format
	 * @return the formatted JSON string
	 */
	private String formatJson (Map<String, Object> data) {
		StringBuilder sb = new StringBuilder();
		formatObject(data, sb, 0);
		return sb.toString();
	}

	/**
	 * Formats an object into a standard JSON string with proper indentation.
	 *
	 * @param obj    the object to format
	 * @param sb     the string builder to append to
	 * @param indent the current indentation level
	 */
	private void formatObject (Object obj, StringBuilder sb, int indent) {
		if (obj instanceof Map<?, ?> map) {
			sb.append("{\n");
			boolean first = true;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				String key = entry.getKey().toString();
				// Skip comment entries since JSON doesn't support comments
				if (key.startsWith("__")) continue;

				if (!first) {
					sb.append(",\n");
				}
				first = false;
				indent(sb, indent + 1);
				sb.append("\"").append(key).append("\": ");
				formatValue(entry.getValue(), sb, indent + 1);
			}
			sb.append("\n");
			indent(sb, indent);
			sb.append("}");
		} else {
			formatValue(obj, sb, indent);
		}
	}

	/**
	 * Formats a value into a standard JSON string.
	 *
	 * @param value  the value to format
	 * @param sb     the string builder to append to
	 * @param indent the current indentation level
	 */
	private void formatValue (Object value, StringBuilder sb, int indent) {
		if (value == null) {
			sb.append("null");
		} else if (value instanceof String) {
			sb.append("\"").append(escapeString((String) value)).append("\"");
		} else if (value instanceof Number || value instanceof Boolean) {
			sb.append(value);
		} else if (value instanceof Iterable<?>) {
			sb.append("[\n");
			boolean first = true;
			for (Object item : (Iterable<?>) value) {
				if (!first) {
					sb.append(",\n");
				}
				first = false;
				indent(sb, indent + 1);
				formatValue(item, sb, indent + 1);
			}
			sb.append("\n");
			indent(sb, indent);
			sb.append("]");
		} else if (value instanceof Map<?, ?>) {
			formatObject(value, sb, indent);
		} else {
			sb.append("\"").append(value).append("\"");
		}
	}

	/**
	 * Escapes a string according to JSON standards.
	 *
	 * @param str the string to escape
	 * @return the escaped string
	 */
	private String escapeString (String str) {
		return str.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\b", "\\b")
				.replace("\f", "\\f")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}

	/**
	 * Adds indentation to the string builder.
	 *
	 * @param sb     the string builder to append to
	 * @param indent the number of indentation levels
	 */
	private void indent (StringBuilder sb, int indent) {
		sb.append("    ".repeat(indent));
	}
} 