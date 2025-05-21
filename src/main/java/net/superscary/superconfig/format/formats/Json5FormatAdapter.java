package net.superscary.superconfig.format.formats;

import net.superscary.superconfig.core.ConfigSerializer;
import net.superscary.superconfig.format.AbstractConfigFormatAdapter;
import net.superscary.superconfig.format.ConfigFormatType;
import net.superscary.superconfig.format.features.Json5Features;
import net.superscary.superconfig.format.tokenizer.Json5Tokenizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * JSON5 format adapter implementation.
 * <p>
 * This adapter handles reading and writing configuration files in JSON5 format,
 * which is a superset of JSON that includes comments, trailing commas, and other
 * features for better readability.
 * </p>
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class Json5FormatAdapter extends AbstractConfigFormatAdapter {
	private Json5Tokenizer tokenizer;
	private final ConfigSerializer serializer;
	private final Json5Features features;

	/**
	 * Creates a new Json5FormatAdapter with configurable features.
	 * JSON5 supports comments, trailing commas, and other extensions.
	 */
	public Json5FormatAdapter () {
		this.features = Json5Features.builder().build();
		this.tokenizer = new Json5Tokenizer(features);
		this.serializer = new ConfigSerializer();
	}

	/**
	 * Sets the known fields for the tokenizer to validate against.
	 * This is used to detect unmapped fields in the configuration file.
	 *
	 * @param knownFields the set of known field names
	 */
	public void setKnownFields (Set<String> knownFields) {
		this.tokenizer = new Json5Tokenizer(features, knownFields);
	}

	@Override
	public String extension () {
		return ConfigFormatType.JSON5.getExtension();
	}

	@Override
	public String lineCommentPrefix () {
		return "//";
	}

	@Override
	public Map<String, Object> read (Path file) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file)) {
			return tokenizer.parse(reader);
		}
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

	private String formatJson (Map<String, Object> data) {
		StringBuilder sb = new StringBuilder();
		formatObject(data, sb, 0);
		return sb.toString();
	}

	private void formatObject (Object obj, StringBuilder sb, int indent) {
		if (obj instanceof Map<?, ?> map) {
			sb.append("{\n");
			boolean first = true;

			// First collect all comments
			Map<String, List<String>> comments = new HashMap<>();

			// Collect class-level comments
			if (map.containsKey("__class_comments")) {
				@SuppressWarnings("unchecked")
				List<String> classComments = (List<String>) map.get("__class_comments");
				comments.put("__class", classComments);
			}

			// Collect field comments and inner class comments
			for (Object key : new ArrayList<>(map.keySet())) {
				String keyStr = key.toString();
				if (keyStr.startsWith("__field_comments_")) {
					@SuppressWarnings("unchecked")
					List<String> fieldComments = (List<String>) map.get(key);
					String fieldName = keyStr.substring("__field_comments_".length());
					comments.put(fieldName, fieldComments);
				} else if (keyStr.startsWith("__class_comments_")) {
					@SuppressWarnings("unchecked")
					List<String> classComments = (List<String>) map.get(key);
					String className = keyStr.substring("__class_comments_".length());
					comments.put("__class_" + className, classComments);
				}
			}

			// Write class-level comments
			if (comments.containsKey("__class")) {
				for (String comment : comments.get("__class")) {
					indent(sb, indent + 1);
					sb.append(lineCommentPrefix()).append(" ").append(comment).append("\n");
				}
			}

			// Then process the actual data
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				String key = entry.getKey().toString();
				// Skip comment entries
				if (key.startsWith("__")) continue;

				if (!first) {
					sb.append(",\n");
				}
				first = false;

				// Write inner class comments if any
				if (comments.containsKey("__class_" + key)) {
					for (String comment : comments.get("__class_" + key)) {
						indent(sb, indent + 1);
						sb.append(lineCommentPrefix()).append(" ").append(comment).append("\n");
					}
				}

				// Write field comments if any
				if (comments.containsKey(key)) {
					for (String comment : comments.get(key)) {
						indent(sb, indent + 1);
						sb.append(lineCommentPrefix()).append(" ").append(comment).append("\n");
					}
				}

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

	private void formatValue (Object value, StringBuilder sb, int indent) {
		if (value == null) {
			sb.append("null");
		} else if (value instanceof String) {
			sb.append("\"").append(escapeString((String) value)).append("\"");
		} else if (value instanceof Number || value instanceof Boolean) {
			sb.append(value);
		} else if (value instanceof Iterable<?>) {
			formatArray((Iterable<?>) value, sb, indent);
		} else if (value instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) value;
			formatObject(map, sb, indent);
		} else {
			sb.append("\"").append(value).append("\"");
		}
	}

	private void formatArray (Iterable<?> array, StringBuilder sb, int indent) {
		sb.append("[\n");
		boolean first = true;
		for (Object item : array) {
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
	}

	private void indent (StringBuilder sb, int indent) {
		sb.append("  ".repeat(indent));
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
		return tokenizer.parse(reader);
	}

	@Override
	protected void writeObject (Object obj, BufferedWriter writer, int indent) throws IOException, IllegalAccessException {
		indent(writer, indent);
		writer.write("{");
		writer.newLine();

		Field[] fields = obj.getClass().getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];

			if (ignoreCheck(field)) continue;

			field.setAccessible(true);
			String key = field.getName().toLowerCase();
			Object value = getFieldValue(field, obj);

			// Write comments
			String[] comments = getFieldComments(field);
			if (comments != null && features.isAllowComments()) {
				for (String comment : comments) {
					writeComment(comment, writer, indent + 1);
				}
			}

			indent(writer, indent + 1);
			if (features.isAllowUnquotedKeys() && isValidUnquotedKey(key)) {
				writer.write(key);
			} else {
				writer.write("\"" + escapeString(key) + "\"");
			}
			writer.write(": ");

			if (value == null) {
				writer.write("null");
			} else if (isSimpleType(value)) {
				writeSimpleValue(value, writer);
			} else if (isCollection(value)) {
				writeCollection((Collection<?>) value, writer, indent + 1);
			} else if (isArray(value)) {
				writeArray(value, writer, indent + 1);
			} else if (isNestedConfig(value)) {
				writeObject(value, writer, indent + 1);
			}

			if (i < fields.length - 1 || features.isAllowTrailingCommas()) {
				writer.write(",");
			}
			writer.newLine();
		}

		indent(writer, indent);
		writer.write("}");
	}

	@Override
	protected void indent (BufferedWriter writer, int levels) throws IOException {
		for (int i = 0; i < levels; i++) {
			writer.write("    ");
		}
	}

	private void writeSimpleValue (Object value, BufferedWriter writer) throws IOException {
		switch (value) {
			case String s -> {
				if (features.isAllowSingleQuotes()) {
					writer.write("'" + escapeString(s) + "'");
				} else {
					writer.write("\"" + escapeString(s) + "\"");
				}
			}
			case Character c -> {
				if (features.isAllowSingleQuotes()) {
					writer.write("'" + escapeChar(c) + "'");
				} else {
					writer.write("\"" + escapeChar(c) + "\"");
				}
			}
			case Number ignored -> {
				String numStr = value.toString();
				if (features.isAllowLeadingZeros() && numStr.startsWith("0") && numStr.length() > 1) {
					writer.write(numStr);
				} else if (features.isAllowLeadingDecimalPoint() && numStr.startsWith(".")) {
					writer.write("0" + numStr);
				} else {
					writer.write(numStr);
				}
			}
			default -> writer.write(value.toString());
		}
	}

	private void writeCollection (Collection<?> collection, BufferedWriter writer, int indent) throws IOException, IllegalAccessException {
		writer.write("[");
		if (!collection.isEmpty()) {
			writer.newLine();
			Iterator<?> it = collection.iterator();
			while (it.hasNext()) {
				Object element = it.next();
				indent(writer, indent + 1);

				if (element == null) {
					writer.write("null");
				} else if (isSimpleType(element)) {
					writeSimpleValue(element, writer);
				} else if (isCollection(element)) {
					writeCollection((Collection<?>) element, writer, indent + 1);
				} else if (isArray(element)) {
					writeArray(element, writer, indent + 1);
				} else if (isNestedConfig(element)) {
					writeObject(element, writer, indent + 1);
				}

				if (it.hasNext() || features.isAllowTrailingCommas()) {
					writer.write(",");
				}
				writer.newLine();
			}
			indent(writer, indent);
		}
		writer.write("]");
	}

	private void writeArray (Object array, BufferedWriter writer, int indent) throws IOException, IllegalAccessException {
		int length = java.lang.reflect.Array.getLength(array);
		writer.write("[");
		if (length > 0) {
			writer.newLine();
			for (int i = 0; i < length; i++) {
				Object element = java.lang.reflect.Array.get(array, i);
				indent(writer, indent + 1);

				if (element == null) {
					writer.write("null");
				} else if (isSimpleType(element)) {
					writeSimpleValue(element, writer);
				} else if (isCollection(element)) {
					writeCollection((Collection<?>) element, writer, indent + 1);
				} else if (isArray(element)) {
					writeArray(element, writer, indent + 1);
				} else if (isNestedConfig(element)) {
					writeObject(element, writer, indent + 1);
				}

				if (i < length - 1 || features.isAllowTrailingCommas()) {
					writer.write(",");
				}
				writer.newLine();
			}
			indent(writer, indent);
		}
		writer.write("]");
	}

	private String escapeChar (char c) {
		if (c == '\\') return "\\\\";
		if (c == '\'') return "\\'";
		if (c == '\b') return "\\b";
		if (c == '\f') return "\\f";
		if (c == '\n') return "\\n";
		if (c == '\r') return "\\r";
		if (c == '\t') return "\\t";
		return String.valueOf(c);
	}

	private boolean isValidUnquotedKey (String key) {
		if (key.isEmpty()) return false;
		if (!Character.isJavaIdentifierStart(key.charAt(0))) return false;
		for (int i = 1; i < key.length(); i++) {
			if (!Character.isJavaIdentifierPart(key.charAt(i))) return false;
		}
		return true;
	}
} 