package net.superscary.superconfig.writer;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Interface for writing config objects to a file.
 * <p>
 * This interface is used by {@link net.superscary.superconfig.manager.ConfigManager} to write
 * config objects to a file. Implementations of this interface should be stateless and thread-safe.
 * </p>
 *
 * @param <T> the type of the config object
 * @author SuperScary
 * @see net.superscary.superconfig.manager.ConfigManager
 * @see net.superscary.superconfig.annotations.Config
 * @see net.superscary.superconfig.annotations.Comment
 * @since 1.0.2
 */
@FunctionalInterface
public interface ConfigWriter<T> {

	/**
	 * Writes the given object to the given writer.
	 *
	 * @param obj    the object to write
	 * @param w      the writer to write to
	 * @param indent the current indent level
	 * @throws IOException            if an I/O error occurs
	 * @throws IllegalAccessException if the object is not accessible
	 */
	void writeObject (T obj, BufferedWriter w, int indent) throws IOException, IllegalAccessException;

}