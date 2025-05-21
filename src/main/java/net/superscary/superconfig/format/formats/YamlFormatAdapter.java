package net.superscary.superconfig.format.formats;

import net.superscary.superconfig.core.ConfigSerializer;
import net.superscary.superconfig.format.AbstractConfigFormatAdapter;
import net.superscary.superconfig.format.ConfigFormatType;
import net.superscary.superconfig.format.features.YamlFeatures;
import net.superscary.superconfig.format.tokenizer.YamlTokenizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * YAML format adapter implementation.
 * <p>
 * This adapter handles reading and writing configuration files in YAML format,
 * which is a human-readable data serialization format.
 * </p>
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class YamlFormatAdapter extends AbstractConfigFormatAdapter {
	private YamlTokenizer tokenizer;
	private final ConfigSerializer serializer;
	private final YamlFeatures features;

	/**
	 * Creates a new YamlFormatAdapter with configurable features.
	 * YAML supports anchors, tags, flow and block styles, and comments.
	 */
	public YamlFormatAdapter () {
		this.features = YamlFeatures.builder().build();
		this.tokenizer = new YamlTokenizer(null, features);
		this.serializer = new ConfigSerializer();
	}

	/**
	 * Sets the known fields for the tokenizer to validate against.
	 * This is used to detect unmapped fields in the configuration file.
	 *
	 * @param knownFields the set of known field names
	 */
	public void setKnownFields (Set<String> knownFields) {
		this.tokenizer = new YamlTokenizer(null, features, knownFields);
	}

	@Override
	public String extension () {
		return ConfigFormatType.YAML.getFileExtension();
	}

	@Override
	public String lineCommentPrefix () {
		return "# ";
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
			String yaml = formatYaml(data);
			Files.writeString(file, yaml);
		} catch (IllegalAccessException e) {
			throw new IOException("Failed to serialize " + cls.getName(), e);
		}
	}

	private String formatYaml (Map<String, Object> data) {
		StringBuilder sb = new StringBuilder();
		formatNode(data, sb, 0);
		return sb.toString();
	}

	private void formatNode (Object obj, StringBuilder sb, int indent) {
		if (obj instanceof Map<?, ?> map) {
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
					indent(sb, indent);
					sb.append(lineCommentPrefix()).append(" ").append(comment).append("\n");
				}
			}

			// Then process the actual data
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				String key = entry.getKey().toString();
				// Skip comment entries
				if (key.startsWith("__")) continue;

				Object value = entry.getValue();

				// Write inner class comments if any
				if (comments.containsKey("__class_" + key)) {
					for (String comment : comments.get("__class_" + key)) {
						indent(sb, indent);
						sb.append(lineCommentPrefix()).append(" ").append(comment).append("\n");
					}
				}

				// Write field comments if any
				if (comments.containsKey(key)) {
					for (String comment : comments.get(key)) {
						indent(sb, indent);
						sb.append(lineCommentPrefix()).append(" ").append(comment).append("\n");
					}
				}

				indent(sb, indent);
				sb.append(key).append(": ");
				if (value == null) {
					sb.append("null\n");
				} else if (value instanceof String) {
					formatString((String) value, sb);
					sb.append("\n");
				} else if (value instanceof Number || value instanceof Boolean) {
					sb.append(value).append("\n");
				} else if (value instanceof List<?>) {
					formatSequence((List<?>) value, sb, indent);
				} else if (value instanceof Map<?, ?>) {
					sb.append("\n");
					formatNode(value, sb, indent + 1);
				} else {
					sb.append(value).append("\n");
				}
			}
		} else {
			formatValue(obj, sb);
		}
	}

	private void formatString (String str, StringBuilder sb) {
		if (str.contains("\n") || str.contains(":") || str.contains("#")) {
			sb.append("|\n");
			indent(sb, 2);
			sb.append(str.replace("\n", "\n  "));
		} else {
			sb.append('"').append(escapeString(str)).append('"');
		}
	}

	private void formatSequence (List<?> sequence, StringBuilder sb, int indent) {
		for (Object item : sequence) {
			indent(sb, indent);
			sb.append("- ");

			if (item == null) {
				sb.append("null");
			} else if (item instanceof String) {
				formatString((String) item, sb);
			} else if (item instanceof Number || item instanceof Boolean) {
				sb.append(item);
			} else if (item instanceof List<?>) {
				sb.append("\n");
				formatSequence((List<?>) item, sb, indent + 2);
			} else if (item instanceof Map<?, ?>) {
				sb.append("\n");
				formatNode(item, sb, indent + 2);
			} else {
				sb.append("\"").append(item).append("\"");
			}
			sb.append("\n");
		}
	}

	private void indent (StringBuilder sb, int levels) {
		sb.append("  ".repeat(levels));
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
			writer.write(": ");

			if (value == null) {
				writer.write("null");
			} else if (isSimpleType(value)) {
				writeSimpleValue(value, writer);
			} else if (isCollection(value)) {
				writer.newLine();
				writeCollection((Collection<?>) value, writer, indent + 2);
			} else if (isArray(value)) {
				writer.newLine();
				writeArray(value, writer, indent + 2);
			} else if (isNestedConfig(value)) {
				writer.newLine();
				writeObject(value, writer, indent + 2);
			}

			writer.newLine();
		}
	}

	@Override
	protected void indent (BufferedWriter writer, int levels) throws IOException {
		writer.write("  ".repeat(levels));
	}

	private void writeSimpleValue (Object value, BufferedWriter writer) throws IOException {
		if (value instanceof String str) {
			if (str.contains("\n") || str.contains(":") || str.contains("#")) {
				writer.write("|\n");
				indent(writer, 2);
				writer.write(str.replace("\n", "\n  "));
			} else {
				writer.write("\"" + escapeString(str) + "\"");
			}
		} else if (value instanceof Number || value instanceof Boolean) {
			writer.write(value.toString());
		} else {
			writer.write("\"" + value.toString() + "\"");
		}
	}

	private void writeCollection (Collection<?> collection, BufferedWriter writer, int indent) throws IOException, IllegalAccessException {
		for (Object element : collection) {
			indent(writer, indent);
			writer.write("- ");

			if (element == null) {
				writer.write("null");
			} else if (isSimpleType(element)) {
				writeSimpleValue(element, writer);
			} else if (isCollection(element)) {
				writer.newLine();
				writeCollection((Collection<?>) element, writer, indent + 2);
			} else if (isArray(element)) {
				writer.newLine();
				writeArray(element, writer, indent + 2);
			} else if (isNestedConfig(element)) {
				writer.newLine();
				writeObject(element, writer, indent + 2);
			}

			writer.newLine();
		}
	}

	private void writeArray (Object array, BufferedWriter writer, int indent) throws IOException, IllegalAccessException {
		int length = java.lang.reflect.Array.getLength(array);
		for (int i = 0; i < length; i++) {
			Object element = java.lang.reflect.Array.get(array, i);
			indent(writer, indent);
			writer.write("- ");

			if (element == null) {
				writer.write("null");
			} else if (isSimpleType(element)) {
				writeSimpleValue(element, writer);
			} else if (isCollection(element)) {
				writer.newLine();
				writeCollection((Collection<?>) element, writer, indent + 2);
			} else if (isArray(element)) {
				writer.newLine();
				writeArray(element, writer, indent + 2);
			} else if (isNestedConfig(element)) {
				writer.newLine();
				writeObject(element, writer, indent + 2);
			}

			writer.newLine();
		}
	}

	private void formatValue (Object value, StringBuilder sb) {
		if (value == null) {
			sb.append("null");
		} else if (value instanceof String) {
			formatString((String) value, sb);
		} else if (value instanceof Number || value instanceof Boolean) {
			sb.append(value);
		} else if (value instanceof List<?>) {
			formatSequence((List<?>) value, sb, 0);
		} else if (value instanceof Map<?, ?>) {
			formatNode(value, sb, 0);
		} else {
			sb.append("\"").append(value).append("\"");
		}
	}
} 