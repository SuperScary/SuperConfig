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
 * Implementations of this interface should be stateless and thread-safe.
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
	 * @return the file extension for this format, e.g. ".json" or ".yaml"
	 */
	String extension ();

	/**
	 * @return the {@link ObjectMapper} for this format. This is used to read and write config files.
	 */
	ObjectMapper getMapper ();

	/**
	 * @param file the file to read from
	 * @return the {@link JsonNode} for this format. This is used to read and write config files.
	 * @throws IOException if an I/O error occurs
	 */
	JsonNode readTree (Path file) throws IOException;

	/**
	 * @param file   the file to write to
	 * @param config the config object to write
	 * @throws IOException if an I/O error occurs
	 */
	<T> void write (Path file, T config, Class<T> writer) throws IOException;

	/**
	 * Single-line comment prefix (e.g. “// ”, “# ”)
	 */
	String lineCommentPrefix ();

	/**
	 * (optional) suffix for comment (only used by formats like XML)
	 */
	default String lineCommentSuffix () {
		return "";
	}

	default boolean ignoreCheck (Field field) {
		return field.isAnnotationPresent(Ignore.class);
	}

}
