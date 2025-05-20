package net.superscary.superconfig.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.superscary.superconfig.annotations.Ignore;
import net.superscary.superconfig.writer.ConfigWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;

/**
 * Interface for config formats. This is used to read and write config files in different formats.
 * <p>
 * Implementations of this interface should be stateless and thread-safe. Each implementation
 * provides support for a specific file format (e.g., JSON, YAML, TOML, XML) and handles the
 * serialization and deserialization of configuration objects to and from that format.
 * </p>
 *
 * @author SuperScary
 * @see net.superscary.superconfig.manager.ConfigManager
 * @see net.superscary.superconfig.annotations.Config
 * @see net.superscary.superconfig.annotations.Comment
 * @since 1.0.2
 */
public interface ConfigFormat {

	/**
	 * Gets the file extension for this format.
	 * <p>
	 * The extension should include the leading dot (e.g., ".json", ".yaml").
	 * </p>
	 *
	 * @return the file extension for this format
	 */
	String extension ();

	/**
	 * Gets the ObjectMapper for this format.
	 * <p>
	 * The ObjectMapper is used to read and write config files in this format.
	 * It should be configured with appropriate settings for the format.
	 * </p>
	 *
	 * @return the ObjectMapper for this format
	 */
	ObjectMapper getMapper ();

	/**
	 * Reads a configuration file and returns its contents as a JsonNode.
	 * <p>
	 * This method should handle any format-specific parsing and return a JsonNode
	 * that represents the contents of the file.
	 * </p>
	 *
	 * @param file the file to read from
	 * @return the JsonNode representing the file contents
	 * @throws IOException if an I/O error occurs
	 */
	JsonNode readTree (Path file) throws IOException;

	/**
	 * Writes a configuration object to a file in this format.
	 * <p>
	 * This method should handle any format-specific serialization and write the
	 * configuration object to the specified file.
	 * </p>
	 *
	 * @param file   the file to write to
	 * @param config the config object to write
	 * @param writer the class of the config object
	 * @param <T>    the type of the config object
	 * @throws IOException if an I/O error occurs
	 */
	<T> void write (Path file, T config, Class<T> writer) throws IOException;

	/**
	 * Gets the prefix for single-line comments in this format.
	 * <p>
	 * For example, "// " for JSON or "# " for YAML.
	 * </p>
	 *
	 * @return the comment prefix
	 */
	String lineCommentPrefix ();

	/**
	 * Gets the suffix for single-line comments in this format.
	 * <p>
	 * This is only used by formats like XML that require a comment suffix.
	 * Default implementation returns an empty string.
	 * </p>
	 *
	 * @return the comment suffix
	 */
	default String lineCommentSuffix () {
		return "";
	}

	/**
	 * Checks if a field should be ignored when reading or writing configuration.
	 * <p>
	 * By default, fields annotated with {@link Ignore} are ignored.
	 * </p>
	 *
	 * @param field the field to check
	 * @return true if the field should be ignored
	 */
	default boolean ignoreCheck (Field field) {
		return field.isAnnotationPresent(Ignore.class);
	}

}
