package net.superscary.superconfig.manager;

import net.superscary.superconfig.core.ConfigFactory;
import net.superscary.superconfig.format.ConfigFormatAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the loading and saving of configuration objects.
 * <p>
 * This class provides functionality to load configuration objects from files and save them back
 * to disk. It supports various file formats through the {@link ConfigFormatAdapter} interface and
 * handles the serialization and deserialization of configuration objects.
 * </p>
 *
 * @param <T> the type of configuration object this manager handles
 * @author SuperScary
 * @see net.superscary.superconfig.format.ConfigFormatAdapter
 * @see net.superscary.superconfig.annotations.Config
 * @since 2.0.0
 */
public class ConfigManager<T> {
	/**
	 * The class type of the configuration object.
	 */
	private final Class<T> type;

	/**
	 * The file path where the configuration is stored.
	 */
	private final Path file;

	/**
	 * The format adapter used for reading and writing the configuration.
	 */
	private final ConfigFormatAdapter adapter;

	/**
	 * The factory used for serialization and deserialization.
	 */
	private final ConfigFactory factory;

	/**
	 * Creates a new ConfigManager with the specified type, file, and format.
	 *
	 * @param type    the class type of the configuration object
	 * @param file    the file path where the configuration is stored
	 * @param adapter the format adapter to use for reading and writing
	 */
	public ConfigManager (Class<T> type, Path file, ConfigFormatAdapter adapter) {
		this.type = type;
		this.file = ensureExtension(file, adapter.extension());
		this.adapter = adapter;
		this.factory = new ConfigFactory();
		this.factory.registerAdapter(adapter);
	}

	/**
	 * Creates a new builder for constructing a ConfigManager.
	 *
	 * @param type the class type of the configuration object
	 * @param <T>  the type of configuration object
	 * @return a new builder instance
	 */
	public static <T> Builder<T> builder (Class<T> type) {
		return new Builder<>(type);
	}

	/**
	 * Loads (or creates) the configuration instance.
	 * <p>
	 * If the configuration file exists, it will be loaded and merged with default values.
	 * If the file doesn't exist, a new instance will be created with default values.
	 * </p>
	 *
	 * @return the populated configuration instance
	 * @throws IOException if an I/O error occurs
	 */
	public T load () throws IOException {
		T cfg = instantiate(type);
		if (Files.exists(file)) {
			return factory.load(file, type);
		}
		return cfg;
	}

	/**
	 * Saves the configuration object to disk.
	 * <p>
	 * The configuration will be written to the file specified in the constructor,
	 * using the format specified in the constructor.
	 * </p>
	 *
	 * @param config the configuration object to save
	 * @throws IOException if an I/O error occurs
	 */
	public void save (T config) throws IOException {
		factory.save(file, config);
	}

	// ——— Internals ———

	/**
	 * Creates a new instance of the specified class.
	 *
	 * @param cls the class to instantiate
	 * @param <U> the type of the class
	 * @return a new instance of the class
	 * @throws IllegalStateException if instantiation fails
	 */
	private <U> U instantiate (Class<U> cls) {
		try {
			return cls.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to instantiate " + cls.getName(), e);
		}
	}

	/**
	 * Ensures a file path has the correct extension.
	 *
	 * @param file the file path to check
	 * @param ext  the extension to ensure
	 * @return the file path with the correct extension
	 */
	private static Path ensureExtension (Path file, String ext) {
		String name = file.getFileName().toString();
		if (!name.endsWith(ext)) {
			name = name + ext;
		}
		return (file.getParent() != null)
				? file.getParent().resolve(name)
				: Paths.get(name);
	}

	/**
	 * Builder for constructing ConfigManager instances.
	 *
	 * @param <T> the type of configuration object
	 */
	public static final class Builder<T> {
		private final Class<T> type;
		private Path file;
		private ConfigFormatAdapter adapter;

		private Builder (Class<T> type) {
			this.type = type;
		}

		/**
		 * Sets the file path for the configuration.
		 *
		 * @param file the file path
		 * @return this builder
		 */
		public Builder<T> file (Path file) {
			this.file = file;
			return this;
		}

		/**
		 * Sets the format adapter for the configuration.
		 *
		 * @param adapter the adapter to use
		 * @return this builder
		 */
		public Builder<T> adapter (ConfigFormatAdapter adapter) {
			this.adapter = adapter;
			return this;
		}

		/**
		 * Builds a new ConfigManager instance.
		 *
		 * @return a new ConfigManager
		 * @throws IllegalStateException if the file path or adapter is not set
		 */
		public ConfigManager<T> build () {
			if (file == null) throw new IllegalStateException("file not set");
			if (adapter == null) throw new IllegalStateException("adapter not set");
			return new ConfigManager<>(type, file, adapter);
		}
	}
}