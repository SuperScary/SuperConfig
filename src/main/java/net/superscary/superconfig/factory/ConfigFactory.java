package net.superscary.superconfig.factory;

import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.format.ConfigFormat;
import net.superscary.superconfig.format.formats.Json5Format;
import net.superscary.superconfig.format.formats.TomlFormat;
import net.superscary.superconfig.manager.ConfigManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Factory for loading and saving configs with minimal boilerplate.
 * This class provides static utility methods for loading and saving configuration objects
 * with sensible defaults. It uses TOML as the default format but supports other formats
 * through the ConfigManager.
 *
 * @author SuperScary
 * @see net.superscary.superconfig.manager.ConfigManager
 * @see net.superscary.superconfig.format.ConfigFormat
 * @since 1.1.1
 */
public final class ConfigFactory {
	/**
	 * Default config format to use when none is specified.
	 * This is the format used by {@link ConfigManager} if no format is specified.
	 * Currently set to TOML format.
	 */
	private static final ConfigFormat DEFAULT_FORMAT = new Json5Format();

	/**
	 * Private constructor to prevent instantiation.
	 * This class is a utility class and should not be instantiated.
	 */
	private ConfigFactory() {
		// no instances
	}

	/**
	 * Load a config from {@code <classname>.json} (lowercased), merge defaults,
	 * then immediately save it back out to disk.
	 * This method provides a convenient way to load a configuration with minimal setup.
	 * It uses the class name (lowercased) as the filename and the default format.
	 *
	 * @param type config class annotated with {@link Config}
	 * @param <T>  type of the config
	 * @return populated config instance
	 * @throws IOException            if an I/O error occurs
	 * @throws IllegalAccessException if the object is not accessible
	 */
	public static <T> T load(Class<T> type) throws IOException, IllegalAccessException {
		String filename = type.getSimpleName().toLowerCase() + DEFAULT_FORMAT.extension();
		return load(type, Paths.get(filename));
	}

	/**
	 * Load a config from the given file, merge defaults,
	 * then immediately save it back out to disk.
	 * This method allows specifying a custom file path while still using the default format.
	 * The file extension will be automatically added if not present.
	 *
	 * @param type config class annotated with {@link Config}
	 * @param file path to the config file
	 * @param <T>  type of the config
	 * @return populated config instance
	 * @throws IOException            if an I/O error occurs
	 * @throws IllegalAccessException if the object is not accessible
	 */
	public static <T> T load(Class<T> type, Path file) throws IOException, IllegalAccessException {
		ConfigManager<T> mgr = ConfigManager
				.<T>builder(type)
				.file(file)
				.format(DEFAULT_FORMAT)
				.build();

		T cfg = mgr.load();
		mgr.save(cfg);
		return cfg;
	}

	/**
	 * Save a config instance back to {@code <classname>.json}.
	 * This method provides a convenient way to save a configuration with minimal setup.
	 * It uses the class name (lowercased) as the filename and the default format.
	 *
	 * @param config config instance
	 * @param <T>    type of the config
	 * @throws IOException            if an I/O error occurs
	 * @throws IllegalAccessException if the object is not accessible
	 */
	public static <T> void save(T config) throws IOException, IllegalAccessException {
		@SuppressWarnings("unchecked")
		Class<T> type = (Class<T>) config.getClass();
		String filename = type.getSimpleName().toLowerCase() + DEFAULT_FORMAT.extension();
		save(config, Paths.get(filename));
	}

	/**
	 * Save a config to the specified file (JSON).
	 * This method allows specifying a custom file path while still using the default format.
	 * The file extension will be automatically added if not present.
	 *
	 * @param config config instance
	 * @param file   destination file path
	 * @param <T>    type of the config
	 * @throws IOException if an I/O error occurs
	 */
	public static <T> void save(T config, Path file) throws IOException {
		@SuppressWarnings("unchecked")
		Class<T> type = (Class<T>) config.getClass();
		ConfigManager<T> mgr = ConfigManager
				.<T>builder(type)
				.file(file)
				.format(DEFAULT_FORMAT)
				.build();
		mgr.save(config);
	}
}
