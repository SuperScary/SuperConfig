package net.superscary.superconfig.format.tokenizer;

import net.superscary.superconfig.exceptions.OutOfSyncException;
import net.superscary.superconfig.format.features.TomlFeatures;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Tokenizer for parsing TOML format.
 * Handles the lexical analysis of TOML input and converts it into tokens.
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class TomlTokenizer {
	private final BufferedReader reader;
	private final TomlFeatures features;
	private final Set<String> knownFields;
	private int line = 1;
	private int column = 0;
	private int currentChar = -1;
	private boolean hasReadChar = false;
	private final Map<String, Object> root = new HashMap<>();
	private final Stack<String> currentTable = new Stack<>();

	public TomlTokenizer (BufferedReader reader, TomlFeatures features) {
		this(reader, features, Collections.emptySet());
	}

	public TomlTokenizer (BufferedReader reader, TomlFeatures features, Set<String> knownFields) {
		this.reader = reader;
		this.features = features;
		this.knownFields = knownFields;
	}

	public Map<String, Object> parse () throws IOException {
		while (true) {
			skipWhitespace();
			if (currentChar == -1) break;

			if (currentChar == '[') {
				parseTableHeader();
			} else {
				parseKeyValue();
			}
		}
		return root;
	}

	private void parseTableHeader () throws IOException {
		next(); // Skip '['
		skipWhitespace();
		StringBuilder tableName = new StringBuilder();
		boolean isArrayTable = false;

		if (currentChar == '[') {
			isArrayTable = true;
			next(); // Skip second '['
		}

		while (currentChar != ']' && currentChar != -1) {
			if (currentChar == '.') {
				if (tableName.isEmpty()) {
					throw new IOException("Invalid table name at line " + line);
				}
				tableName.append('.');
			} else if (currentChar == ' ' || currentChar == '\t') {
				skipWhitespace();
				if (currentChar != ']') {
					throw new IOException("Invalid table name at line " + line);
				}
				break;
			} else {
				tableName.append((char) currentChar);
			}
			next();
		}

		if (currentChar != ']') {
			throw new IOException("Unclosed table header at line " + line);
		}

		if (isArrayTable) {
			next(); // Skip ']'
			if (currentChar != ']') {
				throw new IOException("Unclosed array table header at line " + line);
			}
		}

		next(); // Skip ']'
		skipWhitespace();
		if (currentChar != '\n' && currentChar != -1) {
			throw new IOException("Invalid table header at line " + line);
		}

		String[] parts = tableName.toString().split("\\.");
		Map<String, Object> current = root;
		for (int i = 0; i < parts.length - 1; i++) {
			current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new HashMap<>());
		}

		String lastPart = parts[parts.length - 1];
		if (isArrayTable) {
			List<Map<String, Object>> array = (List<Map<String, Object>>) current.computeIfAbsent(lastPart, k -> new ArrayList<>());
			Map<String, Object> newTable = new HashMap<>();
			array.add(newTable);
			currentTable.clear();
			currentTable.push(lastPart);
		} else {
			Map<String, Object> newTable = new HashMap<>();
			current.put(lastPart, newTable);
			currentTable.clear();
			currentTable.push(lastPart);
		}
	}

	private void parseKeyValue () throws IOException {
		String key = parseKey();
		skipWhitespace();

		if (currentChar == '=') {
			next(); // Skip '='
			skipWhitespace();
			Object value = parseValue();

			if (!knownFields.isEmpty() && !knownFields.contains(key)) {
				throw new OutOfSyncException("Unknown field: " + key, line, column);
			}

			Map<String, Object> current = root;
			for (String part : currentTable) {
				current = (Map<String, Object>) current.computeIfAbsent(part, k -> new HashMap<>());
			}
			current.put(key, value);
		} else {
			throw new IOException("Expected '=' after key at line " + line);
		}
	}

	private String parseKey () throws IOException {
		StringBuilder key = new StringBuilder();
		while (currentChar != '=' && currentChar != ' ' && currentChar != '\t' && currentChar != '\n' && currentChar != -1) {
			key.append((char) currentChar);
			next();
		}
		return key.toString();
	}

	private Object parseValue () throws IOException {
		return switch (currentChar) {
			case '"' -> parseString();
			case '\'' -> parseLiteralString();
			case '[' -> parseArray();
			case '{' -> parseInlineTable();
			case 't' -> parseBoolean("true");
			case 'f' -> parseBoolean("false");
			case '-', '+', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> parseNumber();
			default -> throw new IOException("Unexpected character in value at line " + line);
		};
	}

	private String parseString () throws IOException {
		next(); // Skip opening quote
		StringBuilder value = new StringBuilder();
		boolean isMultiline = false;

		if (currentChar == '"' && features.isAllowMultilineStrings()) {
			next(); // Skip second quote
			if (currentChar == '"') {
				isMultiline = true;
				next(); // Skip third quote
				skipWhitespace();
				if (currentChar != '\n') {
					throw new IOException("Multiline string must start with newline at line " + line);
				}
				next(); // Skip newline
			} else {
				return ""; // Empty string
			}
		}

		while (true) {
			if (currentChar == -1) {
				throw new IOException("Unclosed string at line " + line);
			}

			if (currentChar == '"') {
				if (isMultiline) {
					if (peek() == '"' && peek(2) == '"') {
						next(); // Skip first quote
						next(); // Skip second quote
						next(); // Skip third quote
						break;
					}
				} else {
					next(); // Skip closing quote
					break;
				}
			}

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

		return value.toString();
	}

	private String parseLiteralString () throws IOException {
		if (!features.isAllowLiteralStrings()) {
			throw new IOException("Literal strings are not allowed at line " + line);
		}

		next(); // Skip opening quote
		StringBuilder value = new StringBuilder();
		boolean isMultiline = false;

		if (currentChar == '\'' && features.isAllowMultilineStrings()) {
			next(); // Skip second quote
			if (currentChar == '\'') {
				isMultiline = true;
				next(); // Skip third quote
				skipWhitespace();
				if (currentChar != '\n') {
					throw new IOException("Multiline string must start with newline at line " + line);
				}
				next(); // Skip newline
			} else {
				return ""; // Empty string
			}
		}

		while (true) {
			if (currentChar == -1) {
				throw new IOException("Unclosed literal string at line " + line);
			}

			if (currentChar == '\'') {
				if (isMultiline) {
					if (peek() == '\'' && peek(2) == '\'') {
						next(); // Skip first quote
						next(); // Skip second quote
						next(); // Skip third quote
						break;
					}
				} else {
					next(); // Skip closing quote
					break;
				}
			}

			value.append((char) currentChar);
			next();
		}

		return value.toString();
	}

	private List<Object> parseArray () throws IOException {
		next(); // Skip '['
		List<Object> array = new ArrayList<>();
		skipWhitespace();

		if (currentChar == ']') {
			next(); // Skip ']'
			return array;
		}

		while (true) {
			skipWhitespace();
			array.add(parseValue());
			skipWhitespace();

			if (currentChar == ']') {
				next(); // Skip ']'
				break;
			}

			if (currentChar != ',') {
				throw new IOException("Expected ',' or ']' in array at line " + line);
			}
			next(); // Skip ','
		}

		return array;
	}

	private Map<String, Object> parseInlineTable () throws IOException {
		if (!features.isAllowInlineTables()) {
			throw new IOException("Inline tables are not allowed at line " + line);
		}

		next(); // Skip '{'
		Map<String, Object> table = new HashMap<>();
		skipWhitespace();

		if (currentChar == '}') {
			next(); // Skip '}'
			return table;
		}

		while (true) {
			skipWhitespace();
			String key = parseKey();
			skipWhitespace();

			if (currentChar != '=') {
				throw new IOException("Expected '=' after key in inline table at line " + line);
			}
			next(); // Skip '='
			skipWhitespace();

			Object value = parseValue();
			table.put(key, value);
			skipWhitespace();

			if (currentChar == '}') {
				next(); // Skip '}'
				break;
			}

			if (currentChar != ',') {
				throw new IOException("Expected ',' or '}' in inline table at line " + line);
			}
			next(); // Skip ','
		}

		return table;
	}

	private Boolean parseBoolean (String expected) throws IOException {
		for (int i = 0; i < expected.length(); i++) {
			if (currentChar != expected.charAt(i)) {
				throw new IOException("Expected '" + expected + "' at line " + line);
			}
			next();
		}
		return expected.equals("true");
	}

	private Object parseNumber () throws IOException {
		StringBuilder number = new StringBuilder();
		boolean isFloat = false;
		boolean isDateTime = false;

		// Handle sign
		if (currentChar == '+' || currentChar == '-') {
			number.append((char) currentChar);
			next();
		}

		// Handle integer part
		while (Character.isDigit(currentChar)) {
			number.append((char) currentChar);
			next();
		}

		// Handle decimal point
		if (currentChar == '.') {
			isFloat = true;
			number.append('.');
			next();

			// Handle fractional part
			while (Character.isDigit(currentChar)) {
				number.append((char) currentChar);
				next();
			}
		}

		// Handle exponent
		if (currentChar == 'e' || currentChar == 'E') {
			isFloat = true;
			number.append((char) currentChar);
			next();

			if (currentChar == '+' || currentChar == '-') {
				number.append((char) currentChar);
				next();
			}

			while (Character.isDigit(currentChar)) {
				number.append((char) currentChar);
				next();
			}
		}

		// Try parsing as date/time
		String numberStr = number.toString();
		try {
			if (numberStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
				return LocalDate.parse(numberStr);
			} else if (numberStr.matches("\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?")) {
				return LocalTime.parse(numberStr);
			} else if (numberStr.matches("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?")) {
				return LocalDateTime.parse(numberStr.replace(' ', 'T'));
			}
		} catch (DateTimeParseException ignored) {
			// Not a date/time, continue with number parsing
		}

		// Parse as number
		try {
			if (isFloat) {
				return Double.parseDouble(numberStr);
			} else {
				return Long.parseLong(numberStr);
			}
		} catch (NumberFormatException e) {
			throw new IOException("Invalid number format at line " + line);
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

	private int peek (int n) throws IOException {
		reader.mark(n);
		int next = -1;
		for (int i = 0; i < n; i++) {
			next = reader.read();
		}
		reader.reset();
		return next;
	}
} 