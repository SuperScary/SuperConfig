package net.superscary.superconfig.format;

import net.superscary.superconfig.annotations.Ignore;
import net.superscary.superconfig.value.ConfigValue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

/**
 * Interface for config format adapters. This is used to read and write config files in different formats.
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
 * @since 2.0.0
 */
public interface ConfigFormatAdapter {

    /**
     * Gets the file extension for this format.
     * <p>
     * The extension should include the leading dot (e.g., ".json", ".yaml").
     * </p>
     *
     * @return the file extension for this format
     */
    String extension();

    /**
     * Gets the prefix for single-line comments in this format.
     * <p>
     * For example, "// " for JSON or "# " for YAML.
     * </p>
     *
     * @return the comment prefix
     */
    String lineCommentPrefix();

    /**
     * Gets the suffix for single-line comments in this format.
     * <p>
     * This is only used by formats like XML that require a comment suffix.
     * Default implementation returns an empty string.
     * </p>
     *
     * @return the comment suffix
     */
    default String lineCommentSuffix() {
        return "";
    }

    /**
     * Reads a configuration file and returns its contents as a Map.
     * <p>
     * This method should handle any format-specific parsing and return a Map
     * that represents the contents of the file.
     * </p>
     *
     * @param file the file to read from
     * @return the Map representing the file contents
     * @throws IOException if an I/O error occurs
     */
    Map<String, Object> read(Path file) throws IOException;

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
    <T> void write(Path file, T config, Class<T> writer) throws IOException;

    /**
     * Checks if a field should be ignored when reading or writing configuration.
     * <p>
     * By default, fields annotated with {@link Ignore} are ignored.
     * </p>
     *
     * @param field the field to check
     * @return true if the field should be ignored
     */
    default boolean ignoreCheck(Field field) {
        return field.isAnnotationPresent(Ignore.class);
    }

    /**
     * Gets the value from a field, handling ConfigValue wrappers.
     * <p>
     * This helper method extracts the actual value from a field, unwrapping
     * ConfigValue instances if necessary.
     * </p>
     *
     * @param field the field to get the value from
     * @param obj   the object containing the field
     * @return the unwrapped value
     * @throws IllegalAccessException if the field cannot be accessed
     */
    default Object getFieldValue(Field field, Object obj) throws IllegalAccessException {
        field.setAccessible(true);
        Object raw = field.get(obj);
        if (raw instanceof ConfigValue<?> cv) {
            return cv.get();
        }
        return raw;
    }
} 