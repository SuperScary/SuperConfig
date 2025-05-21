package net.superscary.superconfig.format.tokenizer;

import net.superscary.superconfig.exceptions.OutOfSyncException;
import net.superscary.superconfig.format.features.YamlFeatures;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Tokenizer for parsing YAML format.
 * Handles the lexical analysis of YAML input and converts it into tokens.
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class YamlTokenizer {
	private final BufferedReader reader;
	private final YamlFeatures features;
	private final Set<String> knownFields;
	private int line = 1;
	private int column = 0;
	private int currentChar = -1;
	private boolean hasReadChar = false;
	private final Map<String, Object> root = new HashMap<>();
	private final Stack<String> currentPath = new Stack<>();

	public YamlTokenizer (BufferedReader reader, YamlFeatures features) {
		this(reader, features, Collections.emptySet());
	}

	public YamlTokenizer (BufferedReader reader, YamlFeatures features, Set<String> knownFields) {
		this.reader = reader;
		this.features = features;
		this.knownFields = knownFields;
	}

	public Map<String, Object> parse () throws IOException {
		while (true) {
			skipWhitespace();
			if (currentChar == -1) break;

			if (currentChar == '#') {
				skipComment();
				continue;
			}

			if (currentChar == '-') {
				parseListItem();
			} else {
				parseKeyValue();
			}
		}
		return root;
	}

	private void parseKeyValue () throws IOException {
		String key = parseKey();
		skipWhitespace();

		if (currentChar == ':') {
			next(); // Skip ':'
			skipWhitespace();
			Object value = parseValue();

			if (!knownFields.isEmpty() && !knownFields.contains(key)) {
				throw new OutOfSyncException("Unknown field: " + key, line, column);
			}

			Map<String, Object> current = root;
			for (String part : currentPath) {
				current = (Map<String, Object>) current.computeIfAbsent(part, k -> new HashMap<>());
			}
			current.put(key, value);
		} else {
			throw new IOException("Expected ':' after key at line " + line);
		}
	}

	private void parseListItem () throws IOException {
		next(); // Skip '-'
		skipWhitespace();
		Object value = parseValue();

		Map<String, Object> current = root;
		for (String part : currentPath) {
			current = (Map<String, Object>) current.computeIfAbsent(part, k -> new ArrayList<>());
		}

		if (current instanceof List<?>) {
			((List<Object>) current).add(value);
		} else {
			List<Object> list = new ArrayList<>();
			list.add(value);
			current.put(currentPath.isEmpty() ? "items" : currentPath.peek(), list);
		}
	}

	private String parseKey () throws IOException {
		StringBuilder key = new StringBuilder();
		while (currentChar != ':' && currentChar != ' ' && currentChar != '\t' && currentChar != '\n' && currentChar != -1) {
			key.append((char) currentChar);
			next();
		}
		return key.toString();
	}

	private Object parseValue () throws IOException {
		if (currentChar == '|' || currentChar == '>') {
			return parseBlockScalar();
		} else if (currentChar == '"' || currentChar == '\'') {
			return parseQuotedString();
		} else if (currentChar == '[') {
			return parseFlowSequence();
		} else if (currentChar == '{') {
			return parseFlowMapping();
		} else if (currentChar == '-') {
			return parseBlockSequence();
		} else if (currentChar == '&' && features.isAllowAnchors()) {
			return parseAnchor();
		} else if (currentChar == '!' && features.isAllowTags()) {
			return parseTag();
		} else {
			return parsePlainScalar();
		}
	}

	private String parseBlockScalar () throws IOException {
		char style = (char) currentChar;
		next(); // Skip '|' or '>'
		skipWhitespace();

		StringBuilder value = new StringBuilder();
		int indent = column;

		while (currentChar != -1) {
			if (currentChar == '\n') {
				next();
				skipWhitespace();
				if (column <= indent) break;
				if (style == '>') {
					value.append(' ');
				} else {
					value.append('\n');
				}
			} else {
				value.append((char) currentChar);
				next();
			}
		}

		return value.toString().trim();
	}

	private String parseQuotedString () throws IOException {
		char quote = (char) currentChar;
		next(); // Skip quote
		StringBuilder value = new StringBuilder();

		while (currentChar != quote && currentChar != -1) {
			if (currentChar == '\\') {
				next();
				switch (currentChar) {
					case 'b':
						value.append('\b');
						break;
					case 't':
						value.append('\t');
						break;
					case 'n':
						value.append('\n');
						break;
					case 'f':
						value.append('\f');
						break;
					case 'r':
						value.append('\r');
						break;
					case '"':
						value.append('"');
						break;
					case '\'':
						value.append('\'');
						break;
					case '\\':
						value.append('\\');
						break;
					default:
						value.append('\\').append((char) currentChar);
				}
			} else {
				value.append((char) currentChar);
			}
			next();
		}

		if (currentChar != quote) {
			throw new IOException("Unclosed string at line " + line);
		}
		next(); // Skip closing quote
		return value.toString();
	}

	private List<Object> parseFlowSequence () throws IOException {
		next(); // Skip '['
		List<Object> sequence = new ArrayList<>();
		skipWhitespace();

		if (currentChar == ']') {
			next(); // Skip ']'
			return sequence;
		}

		while (true) {
			skipWhitespace();
			sequence.add(parseValue());
			skipWhitespace();

			if (currentChar == ']') {
				next(); // Skip ']'
				break;
			}

			if (currentChar != ',') {
				throw new IOException("Expected ',' or ']' in sequence at line " + line);
			}
			next(); // Skip ','
		}

		return sequence;
	}

	private Map<String, Object> parseFlowMapping () throws IOException {
		next(); // Skip '{'
		Map<String, Object> mapping = new HashMap<>();
		skipWhitespace();

		if (currentChar == '}') {
			next(); // Skip '}'
			return mapping;
		}

		while (true) {
			skipWhitespace();
			String key = parseKey();
			skipWhitespace();

			if (currentChar != ':') {
				throw new IOException("Expected ':' after key in mapping at line " + line);
			}
			next(); // Skip ':'
			skipWhitespace();

			Object value = parseValue();
			mapping.put(key, value);
			skipWhitespace();

			if (currentChar == '}') {
				next(); // Skip '}'
				break;
			}

			if (currentChar != ',') {
				throw new IOException("Expected ',' or '}' in mapping at line " + line);
			}
			next(); // Skip ','
		}

		return mapping;
	}

	private List<Object> parseBlockSequence () throws IOException {
		List<Object> sequence = new ArrayList<>();
		int indent = column;

		while (currentChar == '-') {
			next(); // Skip '-'
			skipWhitespace();
			sequence.add(parseValue());
			skipWhitespace();

			if (currentChar == '\n') {
				next();
				skipWhitespace();
				if (column <= indent) break;
			}
		}

		return sequence;
	}

	private Object parseAnchor () throws IOException {
		next(); // Skip '&'
		StringBuilder anchor = new StringBuilder();
		while (currentChar != ' ' && currentChar != '\t' && currentChar != '\n' && currentChar != -1) {
			anchor.append((char) currentChar);
			next();
		}
		// TODO: Implement anchor reference handling
		return parseValue();
	}

	private Object parseTag () throws IOException {
		next(); // Skip '!'
		StringBuilder tag = new StringBuilder();
		while (currentChar != ' ' && currentChar != '\t' && currentChar != '\n' && currentChar != -1) {
			tag.append((char) currentChar);
			next();
		}
		// TODO: Implement tag handling
		return parseValue();
	}

	private Object parsePlainScalar () throws IOException {
		StringBuilder value = new StringBuilder();
		while (currentChar != ' ' && currentChar != '\t' && currentChar != '\n' && currentChar != -1) {
			value.append((char) currentChar);
			next();
		}

		String str = value.toString();
		if (str.equals("true") || str.equals("false")) {
			return Boolean.parseBoolean(str);
		} else if (str.equals("null")) {
			return null;
		} else {
			try {
				return Long.parseLong(str);
			} catch (NumberFormatException e) {
				try {
					return Double.parseDouble(str);
				} catch (NumberFormatException e2) {
					return str;
				}
			}
		}
	}

	private void skipComment () throws IOException {
		while (currentChar != '\n' && currentChar != -1) {
			next();
		}
	}

	private void skipWhitespace () throws IOException {
		while (currentChar == ' ' || currentChar == '\t' || currentChar == '\n') {
			if (currentChar == '\n') {
				line++;
				column = 0;
			}
			next();
		}
	}

	private void next () throws IOException {
		if (!hasReadChar) {
			currentChar = reader.read();
			hasReadChar = true;
		} else {
			currentChar = reader.read();
		}
		column++;
	}

	private int peek () throws IOException {
		reader.mark(1);
		int next = reader.read();
		reader.reset();
		return next;
	}
} 