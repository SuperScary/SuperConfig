package net.superscary.superconfig.format.tokenizer;

import net.superscary.superconfig.format.tokens.Token;
import net.superscary.superconfig.format.tokens.TokenType;
import net.superscary.superconfig.format.tokens.Tokenizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class KdlTokenizer implements Tokenizer {
    private final BufferedReader reader;
    private int line = 1;
    private int column = 0;
    private int currentChar = -1;
    private boolean hasReadChar = false;

    public KdlTokenizer(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    @Override
    public List<Token> tokenize() throws IOException {
        List<Token> tokens = new ArrayList<>();
        Token token;
        while ((token = nextToken()) != null) {
            tokens.add(token);
        }
        return tokens;
    }

    private Token nextToken() throws IOException {
        skipWhitespace();

        if (currentChar == -1) {
            return null;
        }

        // Handle version marker
        if (currentChar == '/' && peek() == '-') {
            return parseVersionMarker();
        }

        // Handle comments
        if (currentChar == '/' && peek() == '/') {
            return parseSingleLineComment();
        }
        if (currentChar == '/' && peek() == '*') {
            return parseMultiLineComment();
        }

        // Handle slashdash comments
        if (currentChar == '/' && peek() == '-') {
            return parseSlashdashComment();
        }

        // Handle node name (identifier or quoted string)
        if (isIdentifierStart(currentChar) || currentChar == '"' || currentChar == '#') {
            return parseNodeName();
        }

        // Handle values
        if (currentChar == '(') {
            return parseTypeAnnotation();
        }
        if (currentChar == '=') {
            return new Token(TokenType.EQUALS, "=", line, column);
        }
        if (currentChar == '{') {
            return new Token(TokenType.LEFT_BRACE, "{", line, column);
        }
        if (currentChar == '}') {
            return new Token(TokenType.RIGHT_BRACE, "}", line, column);
        }
        if (currentChar == ';') {
            return new Token(TokenType.SEMICOLON, ";", line, column);
        }

        // Handle numbers and keywords
        if (isDigit(currentChar) || currentChar == '+' || currentChar == '-') {
            return parseNumber();
        }
        if (currentChar == '#') {
            return parseKeyword();
        }

        throw new IOException("Unexpected character: " + (char) currentChar);
    }

    private Token parseVersionMarker() throws IOException {
        StringBuilder value = new StringBuilder();
        int startColumn = column;
        
        // Read /-
        value.append((char) currentChar);
        advance();
        value.append((char) currentChar);
        advance();
        
        skipWhitespace();
        
        // Read kdl-version
        while (isIdentifierChar(currentChar)) {
            value.append((char) currentChar);
            advance();
        }
        
        skipWhitespace();
        
        // Read version number
        while (isDigit(currentChar)) {
            value.append((char) currentChar);
            advance();
        }
        
        return new Token(TokenType.VERSION_MARKER, value.toString(), line, startColumn);
    }

    private Token parseNodeName() throws IOException {
        StringBuilder value = new StringBuilder();
        int startColumn = column;

        if (currentChar == '"') {
            return parseQuotedString();
        } else if (currentChar == '#') {
            return parseRawString();
        } else {
            // Parse identifier
            while (isIdentifierChar(currentChar)) {
                value.append((char) currentChar);
                advance();
            }
            return new Token(TokenType.IDENTIFIER, value.toString(), line, startColumn);
        }
    }

    private Token parseQuotedString() throws IOException {
        StringBuilder value = new StringBuilder();
        int startColumn = column;
        advance(); // Skip opening quote

        while (currentChar != '"' && currentChar != -1) {
            if (currentChar == '\\') {
                advance();
                if (currentChar == 'u') {
                    value.append(parseUnicodeEscape());
                } else {
                    value.append(parseEscape());
                }
            } else {
                value.append((char) currentChar);
                advance();
            }
        }

        if (currentChar == -1) {
            throw new IOException("Unterminated string at line " + line);
        }

        advance(); // Skip closing quote
        return new Token(TokenType.STRING, value.toString(), line, startColumn);
    }

    private Token parseRawString() throws IOException {
        StringBuilder value = new StringBuilder();
        int startColumn = column;
        advance(); // Skip #

        // Count # characters
        int hashCount = 0;
        while (currentChar == '#') {
            hashCount++;
            advance();
        }

        if (currentChar != '"') {
            throw new IOException("Expected \" after raw string delimiter");
        }
        advance(); // Skip opening quote

        // Read until matching closing quote and hashes
        while (true) {
            if (currentChar == '"') {
                boolean isEnd = true;
                for (int i = 0; i < hashCount; i++) {
                    if (peek() != '#') {
                        isEnd = false;
                        break;
                    }
                    advance();
                }
                if (isEnd) {
                    advance(); // Skip closing quote
                    break;
                }
            }
            if (currentChar == -1) {
                throw new IOException("Unterminated raw string at line " + line);
            }
            value.append((char) currentChar);
            advance();
        }

        return new Token(TokenType.RAW_STRING, value.toString(), line, startColumn);
    }

    private Token parseNumber() throws IOException {
        StringBuilder value = new StringBuilder();
        int startColumn = column;

        // Handle sign
        if (currentChar == '+' || currentChar == '-') {
            value.append((char) currentChar);
            advance();
        }

        // Handle hex, octal, binary
        if (currentChar == '0') {
            value.append('0');
            advance();
            if (currentChar == 'x' || currentChar == 'X') {
                value.append('x');
                advance();
                while (isHexDigit(currentChar)) {
                    value.append((char) currentChar);
                    advance();
                }
                return new Token(TokenType.NUMBER, value.toString(), line, startColumn);
            } else if (currentChar == 'o' || currentChar == 'O') {
                value.append('o');
                advance();
                while (isOctalDigit(currentChar)) {
                    value.append((char) currentChar);
                    advance();
                }
                return new Token(TokenType.NUMBER, value.toString(), line, startColumn);
            } else if (currentChar == 'b' || currentChar == 'B') {
                value.append('b');
                advance();
                while (isBinaryDigit(currentChar)) {
                    value.append((char) currentChar);
                    advance();
                }
                return new Token(TokenType.NUMBER, value.toString(), line, startColumn);
            }
        }

        // Handle decimal
        while (isDigit(currentChar)) {
            value.append((char) currentChar);
            advance();
        }

        // Handle decimal point
        if (currentChar == '.') {
            value.append('.');
            advance();
            while (isDigit(currentChar)) {
                value.append((char) currentChar);
                advance();
            }
        }

        // Handle exponent
        if (currentChar == 'e' || currentChar == 'E') {
            value.append((char) currentChar);
            advance();
            if (currentChar == '+' || currentChar == '-') {
                value.append((char) currentChar);
                advance();
            }
            while (isDigit(currentChar)) {
                value.append((char) currentChar);
                advance();
            }
        }

        return new Token(TokenType.NUMBER, value.toString(), line, startColumn);
    }

    private Token parseKeyword() throws IOException {
        StringBuilder value = new StringBuilder();
        int startColumn = column;
        advance(); // Skip #

        while (isIdentifierChar(currentChar)) {
            value.append((char) currentChar);
            advance();
        }

        String keyword = value.toString();
        if (keyword.equals("true") || keyword.equals("false")) {
            return new Token(TokenType.BOOLEAN, keyword, line, startColumn);
        } else if (keyword.equals("null")) {
            return new Token(TokenType.NULL, keyword, line, startColumn);
        } else if (keyword.equals("inf") || keyword.equals("-inf") || keyword.equals("nan")) {
            return new Token(TokenType.NUMBER, keyword, line, startColumn);
        }

        throw new IOException("Invalid keyword: " + keyword);
    }

    private Token parseTypeAnnotation() throws IOException {
        StringBuilder value = new StringBuilder();
        int startColumn = column;
        advance(); // Skip (

        skipWhitespace();
        while (currentChar != ')' && currentChar != -1) {
            value.append((char) currentChar);
            advance();
        }

        if (currentChar == -1) {
            throw new IOException("Unterminated type annotation at line " + line);
        }

        advance(); // Skip )
        return new Token(TokenType.TYPE_ANNOTATION, value.toString().trim(), line, startColumn);
    }

    private Token parseSingleLineComment() throws IOException {
        StringBuilder value = new StringBuilder();
        int startColumn = column;
        advance(); // Skip first /
        advance(); // Skip second /

        while (currentChar != '\n' && currentChar != -1) {
            value.append((char) currentChar);
            advance();
        }

        return new Token(TokenType.COMMENT, value.toString(), line, startColumn);
    }

    private Token parseMultiLineComment() throws IOException {
        StringBuilder value = new StringBuilder();
        int startColumn = column;
        advance(); // Skip /
        advance(); // Skip *

        int depth = 1;
        while (depth > 0 && currentChar != -1) {
            if (currentChar == '/' && peek() == '*') {
                depth++;
                advance();
                advance();
            } else if (currentChar == '*' && peek() == '/') {
                depth--;
                advance();
                advance();
            } else {
                value.append((char) currentChar);
                advance();
            }
        }

        if (currentChar == -1) {
            throw new IOException("Unterminated multi-line comment at line " + line);
        }

        return new Token(TokenType.COMMENT, value.toString(), line, startColumn);
    }

    private Token parseSlashdashComment() throws IOException {
        StringBuilder value = new StringBuilder();
        int startColumn = column;
        advance(); // Skip /
        advance(); // Skip -

        while (currentChar != '\n' && currentChar != -1) {
            value.append((char) currentChar);
            advance();
        }

        return new Token(TokenType.COMMENT, value.toString(), line, startColumn);
    }

    private String parseUnicodeEscape() throws IOException {
        advance(); // Skip u
        if (currentChar != '{') {
            throw new IOException("Invalid unicode escape sequence");
        }
        advance(); // Skip {

        StringBuilder hex = new StringBuilder();
        while (isHexDigit(currentChar)) {
            hex.append((char) currentChar);
            advance();
        }

        if (currentChar != '}') {
            throw new IOException("Invalid unicode escape sequence");
        }
        advance(); // Skip }

        try {
            int codePoint = Integer.parseInt(hex.toString(), 16);
            return new String(Character.toChars(codePoint));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid unicode escape sequence");
        }
    }

    private char parseEscape() throws IOException {
        switch (currentChar) {
            case '"': return '"';
            case '\\': return '\\';
            case 'b': return '\b';
            case 'f': return '\f';
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
            case 's': return ' ';
            default: throw new IOException("Invalid escape sequence");
        }
    }

    private void skipWhitespace() throws IOException {
        while (isWhitespace(currentChar)) {
            advance();
        }
    }

    private void advance() throws IOException {
        if (!hasReadChar) {
            currentChar = reader.read();
            hasReadChar = true;
        } else {
            currentChar = reader.read();
        }

        if (currentChar == '\n') {
            line++;
            column = 0;
        } else {
            column++;
        }
    }

    private int peek() throws IOException {
        reader.mark(1);
        int next = reader.read();
        reader.reset();
        return next;
    }

    private boolean isWhitespace(int c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    private boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    private boolean isHexDigit(int c) {
        return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean isOctalDigit(int c) {
        return c >= '0' && c <= '7';
    }

    private boolean isBinaryDigit(int c) {
        return c == '0' || c == '1';
    }

    private boolean isIdentifierStart(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isIdentifierChar(int c) {
        return isIdentifierStart(c) || isDigit(c) || c == '-' || c == '.';
    }
} 