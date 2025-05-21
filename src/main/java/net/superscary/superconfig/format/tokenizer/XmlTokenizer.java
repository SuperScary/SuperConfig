package net.superscary.superconfig.format.tokenizer;

import net.superscary.superconfig.format.features.XmlFeatures;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tokenizer for XML format parsing.
 * <p>
 * This class handles the tokenization of XML input into a structured format
 * that can be processed by the XML format adapter.
 * </p>
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class XmlTokenizer {
	private final BufferedReader reader;
	private final XmlFeatures features;
	private int lineNumber = 0;
	private String currentLine = null;
	private int position = 0;

	public XmlTokenizer (BufferedReader reader, XmlFeatures features) {
		this.reader = reader;
		this.features = features;
	}

	public Map<String, Object> tokenize () throws IOException {
		Map<String, Object> result = new HashMap<>();
		List<String> comments = new ArrayList<>();

		while ((currentLine = reader.readLine()) != null) {
			lineNumber++;
			position = 0;

			// Skip empty lines
			if (currentLine.trim().isEmpty()) {
				continue;
			}

			// Handle comments
			if (features.isAllowComments() && currentLine.trim().startsWith("<!--")) {
				String comment = extractComment();
				if (comment != null) {
					comments.add(comment);
				}
				continue;
			}

			// Handle processing instructions
			if (features.isAllowProcessingInstructions() && currentLine.trim().startsWith("<?")) {
				skipProcessingInstruction();
				continue;
			}

			// Handle root element
			if (currentLine.trim().startsWith("<") && !currentLine.trim().startsWith("</")) {
				String elementName = extractElementName();
				if (elementName != null) {
					Map<String, Object> elementData = parseElement();
					if (!comments.isEmpty()) {
						elementData.put("__class_comments", new ArrayList<>(comments));
						comments.clear();
					}
					result.put(elementName, elementData);
				}
			}
		}

		return result;
	}

	private String extractComment () {
		int start = currentLine.indexOf("<!--");
		int end = currentLine.indexOf("-->", start);

		if (end == -1) {
			// Multi-line comment
			StringBuilder comment = new StringBuilder();
			comment.append(currentLine.substring(start + 4).trim());

			try {
				while ((currentLine = reader.readLine()) != null) {
					lineNumber++;
					end = currentLine.indexOf("-->");
					if (end != -1) {
						comment.append(" ").append(currentLine.substring(0, end).trim());
						break;
					}
					comment.append(" ").append(currentLine.trim());
				}
			} catch (IOException e) {
				return null;
			}

			return comment.toString().trim();
		}

		return currentLine.substring(start + 4, end).trim();
	}

	private void skipProcessingInstruction () {
		int end = currentLine.indexOf("?>");
		if (end == -1) {
			try {
				while ((currentLine = reader.readLine()) != null) {
					lineNumber++;
					end = currentLine.indexOf("?>");
					if (end != -1) {
						break;
					}
				}
			} catch (IOException e) {
				// Ignore
			}
		}
	}

	private String extractElementName () {
		int start = currentLine.indexOf("<");
		int end = currentLine.indexOf(" ", start);
		if (end == -1) {
			end = currentLine.indexOf(">", start);
		}
		if (end == -1) {
			return null;
		}
		return currentLine.substring(start + 1, end);
	}

	private Map<String, Object> parseElement () throws IOException {
		Map<String, Object> elementData = new HashMap<>();
		List<String> comments = new ArrayList<>();
		String pendingElement = null;
		Map<String, Object> pendingData = null;

		while ((currentLine = reader.readLine()) != null) {
			lineNumber++;
			position = 0;

			// Skip empty lines
			if (currentLine.trim().isEmpty()) {
				continue;
			}

			// Handle comments
			if (features.isAllowComments() && currentLine.trim().startsWith("<!--")) {
				String comment = extractComment();
				if (comment != null) {
					comments.add(comment);
				}
				continue;
			}

			// Handle end of element
			if (currentLine.trim().startsWith("</")) {
				// If we have a pending element with comments, add it now
				if (pendingElement != null && pendingData != null && !comments.isEmpty()) {
					pendingData.put("__field_comments_" + pendingElement, new ArrayList<>(comments));
					elementData.put(pendingElement, pendingData);
					comments.clear();
					pendingElement = null;
					pendingData = null;
				}
				break;
			}

			// Handle child element
			if (currentLine.trim().startsWith("<") && !currentLine.trim().startsWith("</")) {
				String elementName = extractElementName();
				if (elementName != null) {
					// If we have a pending element with comments, add it now
					if (pendingElement != null && pendingData != null && !comments.isEmpty()) {
						pendingData.put("__field_comments_" + pendingElement, new ArrayList<>(comments));
						elementData.put(pendingElement, pendingData);
						comments.clear();
					}

					// Start new pending element
					pendingElement = elementName;
					pendingData = parseElement();
				}
				continue;
			}

			// Handle text content
			String trimmedLine = currentLine.trim();
			if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("<")) {
				// If we have a pending element with comments, add it now
				if (pendingElement != null && pendingData != null && !comments.isEmpty()) {
					pendingData.put("__field_comments_" + pendingElement, new ArrayList<>(comments));
					elementData.put(pendingElement, pendingData);
					comments.clear();
				}

				// Add text content
				if (!comments.isEmpty()) {
					elementData.put("__field_comments_value", new ArrayList<>(comments));
					comments.clear();
				}
				elementData.put("value", trimmedLine);
			}
		}

		// Handle any remaining pending element
		if (pendingElement != null && pendingData != null && !comments.isEmpty()) {
			pendingData.put("__field_comments_" + pendingElement, new ArrayList<>(comments));
			elementData.put(pendingElement, pendingData);
		}

		return elementData;
	}
} 