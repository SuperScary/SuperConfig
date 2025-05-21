package net.superscary.superconfig.format.tokenizer;

import net.superscary.superconfig.exceptions.JsonParseException;
import net.superscary.superconfig.exceptions.OutOfSyncException;
import net.superscary.superconfig.format.features.Json5Features;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * A tokenizer for parsing JSON5 format with configurable feature flags.
 * This class handles the lexical analysis of JSON5 input and converts it into tokens.
 */
public class Json5Tokenizer {
	private final Json5Features features;
	private final Set<String> knownFields;
	private Reader reader;
	private int currentChar;
	private int line = 1;
	private int column = 0;
	private boolean hasNextChar = false;

	public Json5Tokenizer (Json5Features features) {
		this(features, Collections.emptySet());
	}

	public Json5Tokenizer (Json5Features features, Set<String> knownFields) {
		this.features = features;
		this.knownFields = knownFields;
	}

	public Map<String, Object> parse (Reader reader) throws IOException {
		this.reader = reader;
		this.currentChar = -1;
		this.line = 1;
		this.column = 0;
		this.hasNextChar = false;
		return parseObject();
	}

	private Object parseValue () throws IOException {
		skipWhitespace();
		int c = peek();
		return switch (c) {
			case '{' -> parseObject();
			case '[' -> parseArray();
			case '"', '\'' -> parseString();
			case 't' -> parseTrue();
			case 'f' -> parseFalse();
			case 'n' -> parseNull();
			case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> parseNumber();
			case '/' -> {
				if (features.isAllowComments()) {
					skipComment();
					yield parseValue();
				}
				throw new JsonParseException("Unexpected character '/'", line, column);
			}
			default -> throw new JsonParseException("Unexpected character: " + (char) c, line, column);
		};
	}

	private Map<String, Object> parseObject () throws IOException {
		Map<String, Object> obj = new HashMap<>();
		expect('{');
		skipWhitespace();

		if (peek() == '}') {
			next();
			return obj;
		}

		while (true) {
			skipWhitespace();
			int keyStartLine = line;
			int keyStartColumn = column;
			String key = parseKey();

			// Check if the field is known
			if (!knownFields.isEmpty() && !knownFields.contains(key)) {
				throw new OutOfSyncException(key, keyStartLine, keyStartColumn);
			}

			skipWhitespace();
			expect(':');
			Object value = parseValue();
			obj.put(key, value);
			skipWhitespace();

			int c = peek();
			if (c == '}') {
				next();
				break;
			} else if (c == ',') {
				next();
				if (peek() == '}' && features.isAllowTrailingCommas()) {
					next();
					break;
				}
			} else {
				throw new JsonParseException("Expected ',' or '}'", line, column);
			}
		}
		return obj;
	}

	private List<Object> parseArray () throws IOException {
		List<Object> array = new ArrayList<>();
		expect('[');
		skipWhitespace();

		if (peek() == ']') {
			next();
			return array;
		}

		while (true) {
			array.add(parseValue());
			skipWhitespace();

			int c = peek();
			if (c == ']') {
				next();
				break;
			} else if (c == ',') {
				next();
				if (peek() == ']' && features.isAllowTrailingCommas()) {
					next();
					break;
				}
			} else {
				throw new JsonParseException("Expected ',' or ']'", line, column);
			}
		}
		return array;
	}

	private String parseKey () throws IOException {
		int c = peek();
		if (c == '"' || c == '\'') {
			return parseString();
		} else if (features.isAllowUnquotedKeys() && isIdentifierStart(c)) {
			return parseUnquotedKey();
		}
		throw new JsonParseException("Invalid key", line, column);
	}

	private String parseString () throws IOException {
		int quote = next();
		if (!features.isAllowSingleQuotes() && quote == '\'') {
			throw new JsonParseException("Single quotes are not allowed", line, column);
		}

		StringBuilder result = new StringBuilder();
		boolean inEscape = false;

		while (true) {
			int c = next();

			if (inEscape) {
				switch (c) {
					case '"', '\'', '\\', '/' -> result.append((char) c);
					case 'b' -> result.append('\b');
					case 'f' -> result.append('\f');
					case 'n' -> result.append('\n');
					case 'r' -> result.append('\r');
					case 't' -> result.append('\t');
					case 'u' -> {
						StringBuilder hex = new StringBuilder();
						for (int i = 0; i < 4; i++) {
							hex.append((char) next());
						}
						result.append((char) Integer.parseInt(hex.toString(), 16));
					}
					default -> throw new JsonParseException("Invalid escape sequence: \\" + (char) c, line, column);
				}
				inEscape = false;
				continue;
			}

			if (c == '\\') {
				inEscape = true;
				continue;
			}

			if (c == quote) {
				break;
			}

			if (c == '\n' || c == '\r') {
				if (!features.isAllowMultiLineStrings()) {
					throw new JsonParseException("Multi-line strings are not allowed", line, column);
				}
			}

			if (c == -1) {
				throw new JsonParseException("Unterminated string", line, column);
			}

			result.append((char) c);
		}

		return result.toString();
	}

	private String parseUnquotedKey () throws IOException {
		if (!features.isAllowUnquotedKeys()) {
			throw new JsonParseException("Unquoted strings are not allowed", line, column);
		}

		StringBuilder result = new StringBuilder();
		boolean firstChar = true;

		while (true) {
			int c = peek();

			if (firstChar) {
				if (!Character.isJavaIdentifierStart((char) c)) {
					throw new JsonParseException("Invalid unquoted string start: " + (char) c, line, column);
				}
				firstChar = false;
			} else if (!Character.isJavaIdentifierPart((char) c)) {
				break;
			}

			result.append((char) next());
		}

		return result.toString();
	}

	private Number parseNumber () throws IOException {
		StringBuilder result = new StringBuilder();
		boolean hasDecimal = false;
		boolean hasExponent = false;
		boolean hasLeadingZero = false;

		// Optional sign
		if (peek() == '-' || peek() == '+') {
			result.append((char) next());
		}

		// Check for leading zero
		if (peek() == '0') {
			hasLeadingZero = true;
			result.append((char) next());
			if (Character.isDigit((char) peek())) {
				if (!features.isAllowLeadingZeros()) {
					throw new JsonParseException("Leading zeros are not allowed", line, column);
				}
			}
		}

		// Integer part
		while (Character.isDigit((char) peek())) {
			result.append((char) next());
		}

		// Optional decimal point
		if (peek() == '.') {
			if (!features.isAllowLeadingDecimalPoint() && result.isEmpty()) {
				throw new JsonParseException("Leading decimal point is not allowed", line, column);
			}
			hasDecimal = true;
			result.append((char) next());
			if (!Character.isDigit((char) peek())) {
				throw new JsonParseException("Expected digit after decimal point", line, column);
			}
			while (Character.isDigit((char) peek())) {
				result.append((char) next());
			}
		}

		// Optional exponent
		if (peek() == 'e' || peek() == 'E') {
			hasExponent = true;
			result.append((char) next());
			if (peek() == '-' || peek() == '+') {
				result.append((char) next());
			}
			if (!Character.isDigit((char) peek())) {
				throw new JsonParseException("Expected digit in exponent", line, column);
			}
			while (Character.isDigit((char) peek())) {
				result.append((char) next());
			}
		}

		String numStr = result.toString();
		try {
			if (hasDecimal || hasExponent) {
				return Double.parseDouble(numStr);
			} else {
				long value = Long.parseLong(numStr);
				if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
					return (int) value;
				}
				return value;
			}
		} catch (NumberFormatException e) {
			throw new JsonParseException("Invalid number: " + numStr, line, column);
		}
	}

	private boolean parseTrue () throws IOException {
		expect('t');
		expect('r');
		expect('u');
		expect('e');
		return true;
	}

	private boolean parseFalse () throws IOException {
		expect('f');
		expect('a');
		expect('l');
		expect('s');
		expect('e');
		return false;
	}

	private Object parseNull () throws IOException {
		expect('n');
		expect('u');
		expect('l');
		expect('l');
		return null;
	}

	private void skipComment () throws IOException {
		int c = next();
		if (c == '/') {
			// Single-line comment
			do {
				c = next();
			} while (c != '\n' && c != '\r' && c != -1);
		} else if (c == '*') {
			// Multi-line comment
			while (true) {
				c = next();
				if (c == -1) {
					throw new JsonParseException("Unterminated comment", line, column);
				}
				if (c == '*' && peek() == '/') {
					next();
					break;
				}
			}
		} else {
			throw new JsonParseException("Invalid comment", line, column);
		}
	}

	private void skipWhitespace () throws IOException {
		while (true) {
			int c = peek();
			if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
				next();
			} else {
				break;
			}
		}
	}

	private int next () throws IOException {
		if (hasNextChar) {
			hasNextChar = false;
			return currentChar;
		}
		currentChar = reader.read();
		if (currentChar == '\n') {
			line++;
			column = 0;
		} else {
			column++;
		}
		return currentChar;
	}

	private int peek () throws IOException {
		if (!hasNextChar) {
			currentChar = reader.read();
			hasNextChar = true;
		}
		return currentChar;
	}

	private void expect (int expected) throws IOException {
		int c = next();
		if (c != expected) {
			throw new JsonParseException("Expected '" + (char) expected + "' but got '" + (char) c + "'", line, column);
		}
	}

	private boolean isIdentifierStart (int c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
	}
} 