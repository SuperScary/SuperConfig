package net.superscary.superconfig.format.tokenizer;

import net.superscary.superconfig.exceptions.JsonParseException;
import net.superscary.superconfig.exceptions.OutOfSyncException;
import net.superscary.superconfig.format.features.JsonFeatures;

import java.util.*;

/**
 * Tokenizer for parsing standard JSON format.
 * This class handles the lexical analysis of JSON input and converts it into tokens.
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class JsonTokenizer {
	private final JsonFeatures features;
	private String input;
	private int pos;
	private int line;
	private int column;
	private final Set<String> knownFields;

	public JsonTokenizer () {
		this(JsonFeatures.getInstance(), Collections.emptySet());
	}

	public JsonTokenizer (JsonFeatures features) {
		this(features, Collections.emptySet());
	}

	public JsonTokenizer (JsonFeatures features, Set<String> knownFields) {
		this.features = features;
		this.knownFields = knownFields;
	}

	public Map<String, Object> parse (String input) {
		this.input = input;
		this.pos = 0;
		this.line = 1;
		this.column = 1;
		return parseObject();
	}

	private Map<String, Object> parseObject () {
		Map<String, Object> obj = new LinkedHashMap<>();
		skipWhitespace();
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
			skipWhitespace();
			Object value = parseValue();
			obj.put(key, value);
			skipWhitespace();

			if (peek() == '}') {
				next();
				break;
			}

			expect(',');
			// Check for trailing comma
			if (peek() == '}' && !features.isAllowTrailingCommas()) {
				throw new JsonParseException("Trailing commas not allowed in standard JSON", line, column);
			}
		}

		return obj;
	}

	private List<Object> parseArray () {
		List<Object> array = new ArrayList<>();
		skipWhitespace();
		expect('[');
		skipWhitespace();

		if (peek() == ']') {
			next();
			return array;
		}

		while (true) {
			skipWhitespace();
			array.add(parseValue());
			skipWhitespace();

			if (peek() == ']') {
				next();
				break;
			}

			expect(',');
			// Check for trailing comma
			if (peek() == ']' && !features.isAllowTrailingCommas()) {
				throw new JsonParseException("Trailing commas not allowed in standard JSON", line, column);
			}
		}

		return array;
	}

	private String parseKey () {
		char c = peek();
		if (c == '"') {
			return parseString();
		}
		if (features.isAllowUnquotedKeys() && isIdentifierStart(c)) {
			return parseUnquotedKey();
		}
		throw new JsonParseException("Expected quoted key", line, column);
	}

	private String parseString () {
		StringBuilder sb = new StringBuilder();
		char quote = next();
		if (quote == '\'' && !features.isAllowSingleQuotes()) {
			throw new JsonParseException("Single quotes not allowed in standard JSON", line, column);
		}

		while (true) {
			char c = next();
			if (c == quote) {
				break;
			} else if (c == '\\') {
				sb.append(parseEscape());
			} else if ((c == '\n' || c == '\r') && !features.isAllowMultiLineStrings()) {
				throw new JsonParseException("Multi-line strings not allowed in standard JSON", line, column);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private String parseUnquotedKey () {
		StringBuilder sb = new StringBuilder();
		char c = peek();
		while (isIdentifierPart(c)) {
			sb.append(next());
			c = peek();
		}
		return sb.toString();
	}

	private char parseEscape () {
		char c = next();
		return switch (c) {
			case '"', '\\', '/' -> c;
			case 'b' -> '\b';
			case 'f' -> '\f';
			case 'n' -> '\n';
			case 'r' -> '\r';
			case 't' -> '\t';
			case 'u' -> parseUnicodeEscape();
			default -> throw new JsonParseException("Invalid escape sequence \\" + c, line, column);
		};
	}

	private char parseUnicodeEscape () {
		StringBuilder hex = new StringBuilder();
		for (int i = 0; i < 4; i++) {
			hex.append(next());
		}
		try {
			return (char) Integer.parseInt(hex.toString(), 16);
		} catch (NumberFormatException e) {
			throw new JsonParseException("Invalid unicode escape \\u" + hex, line, column);
		}
	}

	private Object parseValue () {
		skipWhitespace();
		char c = peek();

		// Check for comments
		if (c == '/' && features.isAllowComments()) {
			skipComment();
			return parseValue();
		}

		return switch (c) {
			case '{' -> parseObject();
			case '[' -> parseArray();
			case '"', '\'' -> parseString();
			case 't' -> parseTrue();
			case 'f' -> parseFalse();
			case 'n' -> parseNull();
			case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> parseNumber();
			default -> throw new JsonParseException("Unexpected character '" + c + "'", line, column);
		};
	}

	private void skipComment () {
		expect('/');
		char c = next();
		if (c == '/') {
			// Single-line comment
			while (peek() != '\n' && peek() != '\r' && peek() != '\0') {
				next();
			}
		} else if (c == '*') {
			// Multi-line comment
			while (true) {
				c = next();
				if (c == '*' && peek() == '/') {
					next();
					break;
				}
				if (c == '\0') {
					throw new JsonParseException("Unterminated comment", line, column);
				}
			}
		} else {
			throw new JsonParseException("Invalid comment", line, column);
		}
	}

	private Boolean parseTrue () {
		expect('t');
		expect('r');
		expect('u');
		expect('e');
		return true;
	}

	private Boolean parseFalse () {
		expect('f');
		expect('a');
		expect('l');
		expect('s');
		expect('e');
		return false;
	}

	private Object parseNull () {
		expect('n');
		expect('u');
		expect('l');
		expect('l');
		return null;
	}

	private Number parseNumber () {
		StringBuilder sb = new StringBuilder();

		if (peek() == '-') {
			sb.append(next());
		}

		if (peek() == '0') {
			sb.append(next());
			if (isDigit(peek())) {
				if (!features.isAllowLeadingZeros()) {
					throw new JsonParseException("Leading zeros not allowed in standard JSON", line, column);
				}
			}
		} else {
			while (isDigit(peek())) {
				sb.append(next());
			}
		}

		if (peek() == '.') {
			if (sb.isEmpty() && !features.isAllowLeadingDecimalPoint()) {
				throw new JsonParseException("Leading decimal point not allowed in standard JSON", line, column);
			}
			sb.append(next());
			if (!isDigit(peek())) {
				throw new JsonParseException("Expected digit after decimal point", line, column);
			}
			while (isDigit(peek())) {
				sb.append(next());
			}
		}

		if (peek() == 'e' || peek() == 'E') {
			sb.append(next());
			if (peek() == '+' || peek() == '-') {
				sb.append(next());
			}
			if (!isDigit(peek())) {
				throw new JsonParseException("Expected digit after exponent", line, column);
			}
			while (isDigit(peek())) {
				sb.append(next());
			}
		}

		String num = sb.toString();
		try {
			if (num.contains(".") || num.contains("e") || num.contains("E")) {
				return Double.parseDouble(num);
			} else {
				return Long.parseLong(num);
			}
		} catch (NumberFormatException e) {
			throw new JsonParseException("Invalid number '" + num + "'", line, column);
		}
	}

	private void skipWhitespace () {
		while (true) {
			char c = peek();
			if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
				next();
			} else {
				break;
			}
		}
	}

	private void expect (char expected) {
		char c = next();
		if (c != expected) {
			throw new JsonParseException("Expected '" + expected + "' but found '" + c + "'", line, column);
		}
	}

	private char peek () {
		return pos < input.length() ? input.charAt(pos) : '\0';
	}

	private char next () {
		char c = peek();
		pos++;
		if (c == '\n') {
			line++;
			column = 1;
		} else {
			column++;
		}
		return c;
	}

	private boolean isDigit (char c) {
		return c >= '0' && c <= '9';
	}

	private boolean isIdentifierStart (char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
	}

	private boolean isIdentifierPart (char c) {
		return isIdentifierStart(c) || (c >= '0' && c <= '9');
	}

} 