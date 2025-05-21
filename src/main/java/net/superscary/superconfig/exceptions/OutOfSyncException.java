package net.superscary.superconfig.exceptions;

/**
 * Exception thrown when a configuration file contains fields that cannot be mapped
 * to the corresponding configuration class. This typically occurs when the configuration
 * file has been modified externally and contains fields that are not defined in the
 * configuration class.
 */
public class OutOfSyncException extends RuntimeException {
	private final String fieldName;
	private final int line;
	private final int column;

	/**
	 * Creates a new OutOfSyncException with the specified field name and location.
	 *
	 * @param fieldName The name of the unmapped field
	 * @param line      The line number where the field was found
	 * @param column    The column number where the field was found
	 */
	public OutOfSyncException (String fieldName, int line, int column) {
		super(String.format("Unmapped field '%s' found at line %d, column %d", fieldName, line, column));
		this.fieldName = fieldName;
		this.line = line;
		this.column = column;
	}

	/**
	 * Gets the name of the unmapped field.
	 *
	 * @return The field name
	 */
	public String getFieldName () {
		return fieldName;
	}

	/**
	 * Gets the line number where the unmapped field was found.
	 *
	 * @return The line number
	 */
	public int getLine () {
		return line;
	}

	/**
	 * Gets the column number where the unmapped field was found.
	 *
	 * @return The column number
	 */
	public int getColumn () {
		return column;
	}
} 