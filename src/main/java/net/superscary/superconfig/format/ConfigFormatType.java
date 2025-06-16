package net.superscary.superconfig.format;

/**
 * Enum representing different configuration file formats.
 *
 * @author SuperScary
 * @since 2.0.0
 */
public enum ConfigFormatType {
	JSON("json"),
	JSON5("json5"),
	YAML("yml"),
	TOML("toml"),
	XML("xml"),
	KDL("kdl");

	private final String fileExtension;

	ConfigFormatType (String fileExtension) {
		this.fileExtension = fileExtension;
	}

	/**
	 * Gets the file extension for this format.
	 *
	 * @return the file extension
	 */
	public String getFileExtension () {
		return fileExtension;
	}

	/**
	 * Gets the format type from a file extension.
	 *
	 * @param extension the file extension to check
	 * @return the matching format type, or null if no match is found
	 */
	public static ConfigFormatType fromExtension (String extension) {
		// Remove dot prefix if present
		String ext = extension.startsWith(".") ? extension.substring(1) : extension;
		for (ConfigFormatType type : values()) {
			if (type.fileExtension.equals(ext)) {
				return type;
			}
		}
		return null;
	}
} 