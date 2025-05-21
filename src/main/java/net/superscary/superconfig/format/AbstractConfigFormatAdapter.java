package net.superscary.superconfig.format;

import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.annotations.Ignore;
import net.superscary.superconfig.value.ConfigValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Map;

/**
 * Abstract base class for configuration format adapters.
 * <p>
 * This class provides common functionality for reading and writing configuration files,
 * including handling of comments, nested objects, and collections.
 * </p>
 *
 * @author SuperScary
 * @since 2.0.0
 */
public abstract class AbstractConfigFormatAdapter implements ConfigFormatAdapter {

	/**
	 * Reads a configuration file and returns its contents as a Map.
	 * <p>
	 * This method reads the file line by line, handling comments and parsing
	 * the format-specific syntax into a Map representation.
	 * </p>
	 *
	 * @param file the file to read from
	 * @return a Map representing the file contents
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public Map<String, Object> read (Path file) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(file)) {
			return parse(reader);
		}
	}

	/**
	 * Writes a configuration object to a file.
	 * <p>
	 * This method converts the object to a Map and then writes it in the
	 * format-specific syntax.
	 * </p>
	 *
	 * @param file   the file to write to
	 * @param config the config object to write
	 * @param writer the class of the config object
	 * @param <T>    the type of the config object
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	public <T> void write (Path file, T config, Class<T> writer) throws IOException {
		try (BufferedWriter bufferedWriter = Files.newBufferedWriter(file,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			writeObject(config, bufferedWriter, 0);
		} catch (IllegalAccessException e) {
			throw new IOException("Failed to serialize " + writer.getName(), e);
		}
	}

	/**
	 * Parses a configuration file from a BufferedReader.
	 * <p>
	 * This method should be implemented by subclasses to handle the specific
	 * format's syntax.
	 * </p>
	 *
	 * @param reader the reader to parse from
	 * @return a Map representing the file contents
	 * @throws IOException if an I/O error occurs
	 */
	protected abstract Map<String, Object> parse (BufferedReader reader) throws IOException;

	/**
	 * Writes a configuration object to a BufferedWriter.
	 * <p>
	 * This method should be implemented by subclasses to handle the specific
	 * format's syntax.
	 * </p>
	 *
	 * @param obj    the object to write
	 * @param writer the writer to write to
	 * @param indent the current indentation level
	 * @throws IOException            if an I/O error occurs
	 * @throws IllegalAccessException if a field cannot be accessed
	 */
	protected abstract void writeObject (Object obj, BufferedWriter writer, int indent)
			throws IOException, IllegalAccessException;

	/**
	 * Writes a comment to a BufferedWriter.
	 * <p>
	 * This method handles the format-specific comment syntax.
	 * </p>
	 *
	 * @param comment the comment to write
	 * @param writer  the writer to write to
	 * @param indent  the current indentation level
	 * @throws IOException if an I/O error occurs
	 */
	protected void writeComment (String comment, BufferedWriter writer, int indent) throws IOException {
		indent(writer, indent);
		writer.write(lineCommentPrefix() + comment + lineCommentSuffix());
		writer.newLine();
	}

	/**
	 * Writes indentation to a BufferedWriter.
	 * <p>
	 * This method should be implemented by subclasses to handle the specific
	 * format's indentation style.
	 * </p>
	 *
	 * @param writer the writer to write to
	 * @param levels the number of indentation levels
	 * @throws IOException if an I/O error occurs
	 */
	protected abstract void indent (BufferedWriter writer, int levels) throws IOException;

	/**
	 * Checks if a field should be ignored when reading or writing configuration.
	 * <p>
	 * By default, fields annotated with {@link Ignore} or with static/transient
	 * modifiers are ignored.
	 * </p>
	 *
	 * @param field the field to check
	 * @return true if the field should be ignored
	 */
	@Override
	public boolean ignoreCheck (Field field) {
		int mods = field.getModifiers();
		return Modifier.isStatic(mods) || Modifier.isTransient(mods) || field.isSynthetic()
				|| field.isAnnotationPresent(Ignore.class);
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
	@Override
	public Object getFieldValue (Field field, Object obj) throws IllegalAccessException {
		field.setAccessible(true);
		Object raw = field.get(obj);
		if (raw instanceof ConfigValue<?> cv) {
			return cv.get();
		}
		return raw;
	}

	/**
	 * Gets the comments for a field.
	 * <p>
	 * This helper method extracts comments from the field's annotations.
	 * </p>
	 *
	 * @param field the field to get comments for
	 * @return an array of comment lines, or null if no comments
	 */
	protected String[] getFieldComments (Field field) {
		Comment comment = field.getAnnotation(Comment.class);
		return comment != null ? comment.value() : null;
	}

	/**
	 * Checks if a value is a simple type that can be written directly.
	 * <p>
	 * Simple types include primitives, strings, and enums.
	 * </p>
	 *
	 * @param value the value to check
	 * @return true if the value is a simple type
	 */
	protected boolean isSimpleType (Object value) {
		return value instanceof Number
				|| value instanceof Boolean
				|| value instanceof String
				|| value instanceof Character
				|| value.getClass().isEnum();
	}

	/**
	 * Checks if a value is a collection type.
	 * <p>
	 * Collection types include Lists, Sets, and Queues.
	 * </p>
	 *
	 * @param value the value to check
	 * @return true if the value is a collection
	 */
	protected boolean isCollection (Object value) {
		return value instanceof Collection<?>;
	}

	/**
	 * Checks if a value is an array.
	 *
	 * @param value the value to check
	 * @return true if the value is an array
	 */
	protected boolean isArray (Object value) {
		return value != null && value.getClass().isArray();
	}

	/**
	 * Checks if a value is a nested configuration object.
	 * <p>
	 * Nested objects are classes annotated with {@link Config}.
	 * </p>
	 *
	 * @param value the value to check
	 * @return true if the value is a nested configuration object
	 */
	protected boolean isNestedConfig (Object value) {
		return value != null && value.getClass().isAnnotationPresent(Config.class);
	}
} 