package net.superscary.superconfig.format.tokens;

public record Token(TokenType type, String value, int line, int column) {
} 