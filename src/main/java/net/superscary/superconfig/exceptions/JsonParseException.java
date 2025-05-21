package net.superscary.superconfig.exceptions;

/**
 * Exception thrown when there is an error parsing JSON input.
 * This exception provides detailed information about where the parsing error occurred.
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class JsonParseException extends RuntimeException {
	private final int line;
	private final int column;

	public JsonParseException (String message, int line, int column) {
		super(String.format("%s at line %d, column %d", message, line, column));
		this.line = line;
		this.column = column;
	}

	public JsonParseException (String message) {
		super(message);
		this.line = -1;
		this.column = -1;
	}

	public int getLine () {
		return line;
	}

	public int getColumn () {
		return column;
	}
} 