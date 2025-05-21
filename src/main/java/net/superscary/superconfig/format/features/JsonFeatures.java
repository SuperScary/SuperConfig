package net.superscary.superconfig.format.features;

/**
 * Features configuration for standard JSON parsing.
 * This class defines the features available in standard JSON format.
 * Unlike JSON5, standard JSON has a fixed set of features that cannot be configured.
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class JsonFeatures {
	private static final JsonFeatures INSTANCE = new JsonFeatures();

	private JsonFeatures () {
		// Private constructor to enforce singleton pattern
	}

	/**
	 * Gets the singleton instance of JsonFeatures.
	 * Standard JSON features are fixed and cannot be configured.
	 *
	 * @return the singleton instance
	 */
	public static JsonFeatures getInstance () {
		return INSTANCE;
	}

	/**
	 * Standard JSON does not support comments.
	 *
	 * @return always false
	 */
	public boolean isAllowComments () {
		return false;
	}

	/**
	 * Standard JSON does not support trailing commas.
	 *
	 * @return always false
	 */
	public boolean isAllowTrailingCommas () {
		return false;
	}

	/**
	 * Standard JSON requires quoted keys.
	 *
	 * @return always false
	 */
	public boolean isAllowUnquotedKeys () {
		return false;
	}

	/**
	 * Standard JSON only allows double quotes for strings.
	 *
	 * @return always false
	 */
	public boolean isAllowSingleQuotes () {
		return false;
	}

	/**
	 * Standard JSON does not allow leading zeros in numbers.
	 *
	 * @return always false
	 */
	public boolean isAllowLeadingZeros () {
		return false;
	}

	/**
	 * Standard JSON does not allow leading decimal points in numbers.
	 *
	 * @return always false
	 */
	public boolean isAllowLeadingDecimalPoint () {
		return false;
	}

	/**
	 * Standard JSON does not support multi-line strings.
	 *
	 * @return always false
	 */
	public boolean isAllowMultiLineStrings () {
		return false;
	}
} 