package net.superscary.superconfig.format.formats;

import net.superscary.superconfig.core.ConfigSerializer;
import net.superscary.superconfig.format.AbstractConfigFormatAdapter;
import net.superscary.superconfig.format.ConfigFormatType;
import net.superscary.superconfig.format.features.TomlFeatures;
import net.superscary.superconfig.format.tokenizer.TomlTokenizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * TOML format adapter implementation.
 * <p>
 * This adapter handles reading and writing configuration files in TOML format,
 * which is a configuration file format that's easy to read due to obvious semantics.
 * </p>
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class TomlFormatAdapter extends AbstractConfigFormatAdapter {
	private TomlTokenizer tokenizer;
	private final ConfigSerializer serializer;
	private final TomlFeatures features;

	/**
	 * Creates a new TomlFormatAdapter with configurable features.
	 * TOML supports tables, arrays, and various data types.
	 */
	public TomlFormatAdapter () {
		this.features = TomlFeatures.builder().build();
		this.tokenizer = new TomlTokenizer(null, features);
		this.serializer = new ConfigSerializer();
	}

	/**
	 * Sets the known fields for the tokenizer to validate against.
	 * This is used to detect unmapped fields in the configuration file.
	 *
	 * @param knownFields the set of known field names
	 */
	public void setKnownFields (Set<String> knownFields) {
		this.tokenizer = new TomlTokenizer(null, features, knownFields);
	}

	@Override
	public String extension () {
		return "." + ConfigFormatType.TOML.getExtension();
	}

	@Override
	public String lineCommentPrefix () {
		return "#";
	}

	@Override
	public Map<String, Object> read (Path file) throws IOException {
		try (BufferedReader ignored = Files.newBufferedReader(file)) {
			return tokenizer.parse();
		}
	}

	@Override
	public <T> void write (Path file, T config, Class<T> cls) throws IOException {
		try {
			Map<String, Object> data = serializer.toMap(config);
			String toml = formatToml(data);
			Files.writeString(file, toml);
		} catch (IllegalAccessException e) {
			throw new IOException("Failed to serialize " + cls.getName(), e);
		}
	}

	private String formatToml (Map<String, Object> data) {
		StringBuilder sb = new StringBuilder();
		formatTable(data, sb, "", true);
		return sb.toString();
	}

	private void formatTable (Map<String, Object> table, StringBuilder sb, String prefix, boolean isRoot) {
		if (!isRoot) {
			// Add class-level comments if available
			if (table.containsKey("__class_comments")) {
				@SuppressWarnings("unchecked")
				List<String> comments = (List<String>) table.get("__class_comments");
				for (String comment : comments) {
					sb.append(lineCommentPrefix()).append(" ").append(comment).append("\n");
				}
			}

			sb.append("\n[");
			if (!prefix.isEmpty()) {
				sb.append(prefix);
			}
			sb.append("]\n");
		}

		// First collect all comments
		Map<String, List<String>> comments = new HashMap<>();

		// Collect field comments
		for (String key : new ArrayList<>(table.keySet())) {
			if (key.startsWith("__field_comments_")) {
				@SuppressWarnings("unchecked")
				List<String> fieldComments = (List<String>) table.get(key);
				String fieldName = key.substring("__field_comments_".length());
				comments.put(fieldName, fieldComments);
			}
		}

		for (Map.Entry<String, Object> entry : table.entrySet()) {
			String key = entry.getKey();
			// Skip comment entries
			if (key.startsWith("__")) continue;

			Object value = entry.getValue();

			// Write field comments if any
			if (comments.containsKey(key)) {
				for (String comment : comments.get(key)) {
					sb.append(lineCommentPrefix()).append(" ").append(comment).append("\n");
				}
			}

			if (value instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> nestedTable = (Map<String, Object>) value;
				String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
				formatTable(nestedTable, sb, newPrefix, false);
			} else {
				formatKeyValue(key, value, sb);
			}
		}
	}

	private void formatKeyValue (String key, Object value, StringBuilder sb) {
		sb.append(key).append(" = ");
		formatValue(value, sb);
		sb.append("\n");
	}

	private void formatValue (Object value, StringBuilder sb) {
		if (value == null) {
			sb.append("null");
		} else if (value instanceof String) {
			formatString((String) value, sb);
		} else if (value instanceof Number || value instanceof Boolean) {
			sb.append(value);
		} else if (value instanceof List<?>) {
			formatArray((List<?>) value, sb);
		} else if (value instanceof Map<?, ?>) {
			formatInlineTable((Map<?, ?>) value, sb);
		} else {
			sb.append("\"").append(value).append("\"");
		}
	}

	private void formatString (String str, StringBuilder sb) {
		if (str.contains("\n") || str.contains("\"") || str.contains("'")) {
			// Use multiline string if needed
			sb.append("\"\"\"\n").append(str).append("\n\"\"\"");
		} else {
			sb.append("\"").append(escapeString(str)).append("\"");
		}
	}

	private void formatArray (List<?> array, StringBuilder sb) {
		sb.append("[");
		for (int i = 0; i < array.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			formatValue(array.get(i), sb);
		}
		sb.append("]");
	}

	private void formatInlineTable (Map<?, ?> table, StringBuilder sb) {
		sb.append("{");
		boolean first = true;
		for (Map.Entry<?, ?> entry : table.entrySet()) {
			if (!first) {
				sb.append(", ");
			}
			first = false;
			sb.append(entry.getKey()).append(" = ");
			formatValue(entry.getValue(), sb);
		}
		sb.append("}");
	}

	private String escapeString (String str) {
		return str.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\b", "\\b")
				.replace("\f", "\\f")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}

	@Override
	protected Map<String, Object> parse (BufferedReader reader) throws IOException {
		return tokenizer.parse();
	}

	@Override
	protected void writeObject (Object obj, BufferedWriter writer, int indent) throws IOException, IllegalAccessException {
		Field[] fields = obj.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (ignoreCheck(field)) continue;

			field.setAccessible(true);
			String key = field.getName().toLowerCase();
			Object value = getFieldValue(field, obj);

			// Write comments
			String[] comments = getFieldComments(field);
			if (comments != null) {
				for (String comment : comments) {
					writeComment(comment, writer, indent);
				}
			}

			indent(writer, indent);
			writer.write(key);
			writer.write(" = ");

			if (value == null) {
				writer.write("null");
			} else if (isSimpleType(value)) {
				writeSimpleValue(value, writer);
			} else if (isCollection(value)) {
				writeCollection((Collection<?>) value, writer);
			} else if (isArray(value)) {
				writeArray(value, writer);
			} else if (isNestedConfig(value)) {
				writer.newLine();
				writeObject(value, writer, indent + 1);
			}

			writer.newLine();
		}
	}

	@Override
	protected void indent (BufferedWriter writer, int levels) throws IOException {
		for (int i = 0; i < levels; i++) {
			writer.write("    ");
		}
	}

	private void writeSimpleValue (Object value, BufferedWriter writer) throws IOException {
		if (value instanceof String str) {
			if (str.contains("\n") || str.contains("\"") || str.contains("'")) {
				writer.write("\"\"\"\n");
				writer.write(str);
				writer.write("\n\"\"\"");
			} else {
				writer.write("\"" + escapeString(str) + "\"");
			}
		} else if (value instanceof Number || value instanceof Boolean) {
			writer.write(value.toString());
		} else {
			writer.write("\"" + value.toString() + "\"");
		}
	}

	private void writeCollection (Collection<?> collection, BufferedWriter writer) throws IOException, IllegalAccessException {
		writer.write("[");
		Iterator<?> it = collection.iterator();
		while (it.hasNext()) {
			Object element = it.next();
			if (element == null) {
				writer.write("null");
			} else if (isSimpleType(element)) {
				writeSimpleValue(element, writer);
			} else if (isCollection(element)) {
				writeCollection((Collection<?>) element, writer);
			} else if (isArray(element)) {
				writeArray(element, writer);
			} else if (isNestedConfig(element)) {
				writer.write("{");
				writer.newLine();
				writeObject(element, writer, 1);
				writer.write("}");
			}

			if (it.hasNext()) {
				writer.write(", ");
			}
		}
		writer.write("]");
	}

	private void writeArray (Object array, BufferedWriter writer) throws IOException, IllegalAccessException {
		int length = java.lang.reflect.Array.getLength(array);
		writer.write("[");
		for (int i = 0; i < length; i++) {
			Object element = java.lang.reflect.Array.get(array, i);
			if (element == null) {
				writer.write("null");
			} else if (isSimpleType(element)) {
				writeSimpleValue(element, writer);
			} else if (isCollection(element)) {
				writeCollection((Collection<?>) element, writer);
			} else if (isArray(element)) {
				writeArray(element, writer);
			} else if (isNestedConfig(element)) {
				writer.write("{");
				writer.newLine();
				writeObject(element, writer, 1);
				writer.write("}");
			}

			if (i < length - 1) {
				writer.write(", ");
			}
		}
		writer.write("]");
	}
} 