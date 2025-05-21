package net.superscary.superconfig.core;

import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.annotations.Ignore;
import net.superscary.superconfig.format.ConfigFormatAdapter;
import net.superscary.superconfig.format.ConfigFormatType;
import net.superscary.superconfig.format.formats.JsonFormatAdapter;
import net.superscary.superconfig.format.formats.Json5FormatAdapter;
import net.superscary.superconfig.format.formats.TomlFormatAdapter;
import net.superscary.superconfig.format.formats.XmlFormatAdapter;
import net.superscary.superconfig.format.formats.YamlFormatAdapter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Factory class for managing configuration format adapters and handling object mapping.
 * <p>
 * This class is responsible for:
 * - Discovering and managing format adapters
 * - Choosing the appropriate adapter based on file extension
 * - Coordinating serialization and deserialization
 * </p>
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class ConfigFactory {
	private final Map<String, ConfigFormatAdapter> adapters;
	private final ConfigSerializer serializer;

	public ConfigFactory () {
		this.adapters = new HashMap<>();
		this.serializer = new ConfigSerializer();
		loadAdapters();
	}

	/**
	 * Loads all available format adapters using the ServiceLoader mechanism.
	 */
	private void loadAdapters () {
		// Register built-in adapters
		adapters.put(ConfigFormatType.JSON.getFileExtension(), new JsonFormatAdapter());
		adapters.put(ConfigFormatType.JSON5.getFileExtension(), new Json5FormatAdapter());
		adapters.put(ConfigFormatType.TOML.getFileExtension(), new TomlFormatAdapter());
		adapters.put(ConfigFormatType.YAML.getFileExtension(), new YamlFormatAdapter());
		adapters.put(ConfigFormatType.XML.getFileExtension(), new XmlFormatAdapter());

		// Load additional adapters from ServiceLoader
		ServiceLoader<ConfigFormatAdapter> loader = ServiceLoader.load(ConfigFormatAdapter.class);
		for (ConfigFormatAdapter adapter : loader) {
			adapters.put(adapter.extension(), adapter);
		}
	}

	/**
	 * Gets the appropriate format adapter for a given format type.
	 *
	 * @param formatType the configuration format type
	 * @return the format adapter for the specified format
	 * @throws IllegalArgumentException if no adapter is found for the format
	 */
	public ConfigFormatAdapter getAdapter (ConfigFormatType formatType) {
		ConfigFormatAdapter adapter = adapters.get(formatType.getFileExtension());
		if (adapter == null) {
			throw new IllegalArgumentException("No adapter found for format: " + formatType);
		}
		return adapter;
	}

	/**
	 * Gets the appropriate format adapter for a given file.
	 *
	 * @param file the configuration file
	 * @return the format adapter for the file's extension
	 * @throws IllegalArgumentException if no adapter is found for the file's extension
	 */
	public ConfigFormatAdapter getAdapter (Path file) {
		String extension = getExtension(file);
		ConfigFormatType formatType = ConfigFormatType.fromExtension(extension);
		if (formatType == null) {
			throw new IllegalArgumentException("Unsupported file extension: " + extension);
		}
		return getAdapter(formatType);
	}

	/**
	 * Gets the configuration file path for a given class.
	 * Uses the name, format, and path from the @Config annotation if present,
	 * otherwise uses the class name and default format in the working directory.
	 *
	 * @param cls the configuration class
	 * @param <T> the type of the configuration class
	 * @return the path to the configuration file
	 * @throws IllegalArgumentException if the class is not annotated with @Config
	 */
	public <T> Path getConfigPath (Class<T> cls) {
		Config config = cls.getAnnotation(Config.class);
		if (config == null) {
			throw new IllegalArgumentException("Class " + cls.getName() + " must be annotated with @Config");
		}

		String name = config.value().isEmpty() ? cls.getSimpleName().toLowerCase() : config.value();
		String extension = config.format().getFileExtension();

		// Ensure the name doesn't already end with the extension
		if (name.endsWith(extension)) {
			name = name.substring(0, name.length() - extension.length());
		}

		// Build the path
		Path basePath = config.path().isEmpty() ? Path.of("") : Path.of(config.path());
		return basePath.resolve(name + extension);
	}

	/**
	 * Loads a configuration object from a file.
	 * If the file doesn't exist, a new instance of the specified class will be created
	 * and returned. If the file exists, it will be parsed and mapped to the specified class.
	 *
	 * @param cls the class of the configuration object
	 * @param <T> the type of the configuration object
	 * @return the loaded configuration object
	 * @throws IOException              if an I/O error occurs
	 * @throws IllegalArgumentException if the class is not annotated with @Config
	 */
	public <T> T load (Class<T> cls) throws IOException {
		Path file = getConfigPath(cls);
		Config config = cls.getAnnotation(Config.class);
		return load(file, cls, config.format());
	}

	/**
	 * Loads a configuration object from a file.
	 * If the file doesn't exist, a new instance of the specified class will be created
	 * and returned. If the file exists, it will be parsed and mapped to the specified class.
	 *
	 * @param file the file to load from
	 * @param cls  the class of the configuration object
	 * @param <T>  the type of the configuration object
	 * @return the loaded configuration object
	 * @throws IOException if an I/O error occurs
	 */
	public <T> T load (Path file, Class<T> cls) throws IOException {
		Config config = cls.getAnnotation(Config.class);
		if (config == null) {
			throw new IllegalArgumentException("Class " + cls.getName() + " must be annotated with @Config");
		}
		return load(file, cls, config.format());
	}

	/**
	 * Loads a configuration object from a file.
	 * If the file doesn't exist, a new instance of the specified class will be created
	 * and returned. If the file exists, it will be parsed and mapped to the specified class.
	 *
	 * @param file       the file to load from
	 * @param cls        the class of the configuration object
	 * @param formatType the format type to use
	 * @param <T>        the type of the configuration object
	 * @return the loaded configuration object
	 * @throws IOException if an I/O error occurs
	 */
	private <T> T load (Path file, Class<T> cls, ConfigFormatType formatType) throws IOException {
		ConfigFormatAdapter adapter = getAdapter(formatType);

		// Create parent directories if they don't exist
		Path parent = file.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}

		// If file doesn't exist, create a new instance
		if (!Files.exists(file)) {
			T instance;
			try {
				instance = cls.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new IOException("Failed to create new instance of " + cls.getName(), e);
			}
			// Save the new instance to create the file
			save(file, instance, formatType);
			return instance;
		}

		// Collect known fields from the configuration class
		Set<String> knownFields = new HashSet<>();
		for (Field field : cls.getDeclaredFields()) {
			if (!field.isAnnotationPresent(Ignore.class) && !Modifier.isStatic(field.getModifiers())) {
				knownFields.add(field.getName().toLowerCase());
			}
		}

		// Update adapter with known fields
		if (adapter instanceof JsonFormatAdapter jsonAdapter) {
			jsonAdapter.setKnownFields(knownFields);
		} else if (adapter instanceof Json5FormatAdapter json5Adapter) {
			json5Adapter.setKnownFields(knownFields);
		}

		// File exists, load and parse it
		Map<String, Object> data = adapter.read(file);
		try {
			return serializer.fromMap(data, cls);
		} catch (ReflectiveOperationException e) {
			throw new IOException("Failed to map data to " + cls.getName(), e);
		}
	}

	/**
	 * Checks if a configuration file exists.
	 *
	 * @param cls the configuration class
	 * @param <T> the type of the configuration class
	 * @return true if the file exists, false otherwise
	 * @throws IllegalArgumentException if the class is not annotated with @Config
	 */
	public <T> boolean exists (Class<T> cls) {
		return exists(getConfigPath(cls));
	}

	/**
	 * Checks if a configuration file exists.
	 *
	 * @param file the file to check
	 * @return true if the file exists, false otherwise
	 */
	public boolean exists (Path file) {
		return Files.exists(file);
	}

	/**
	 * Saves a configuration object to a file.
	 *
	 * @param config the configuration object to save
	 * @param <T>    the type of the configuration object
	 * @throws IOException              if an I/O error occurs
	 * @throws IllegalArgumentException if the class is not annotated with @Config
	 */
	public <T> void save (T config) throws IOException {
		Class<?> cls = config.getClass();
		Config configAnnotation = cls.getAnnotation(Config.class);
		if (configAnnotation == null) {
			throw new IllegalArgumentException("Class " + cls.getName() + " must be annotated with @Config");
		}
		Path file = getConfigPath(cls);
		save(file, config, configAnnotation.format());
	}

	/**
	 * Saves a configuration object to a file.
	 *
	 * @param file   the file to save to
	 * @param config the configuration object to save
	 * @param <T>    the type of the configuration object
	 * @throws IOException if an I/O error occurs
	 */
	public <T> void save (Path file, T config) throws IOException {
		Class<?> cls = config.getClass();
		Config configAnnotation = cls.getAnnotation(Config.class);
		if (configAnnotation == null) {
			throw new IllegalArgumentException("Class " + cls.getName() + " must be annotated with @Config");
		}
		save(file, config, configAnnotation.format());
	}

	/**
	 * Saves a configuration object to a file.
	 *
	 * @param file       the file to save to
	 * @param config     the configuration object to save
	 * @param formatType the format type to use
	 * @param <T>        the type of the configuration object
	 * @throws IOException if an I/O error occurs
	 */
	private <T> void save (Path file, T config, ConfigFormatType formatType) throws IOException {
		ConfigFormatAdapter adapter = getAdapter(formatType);
		adapter.write(file, config, (Class<T>) config.getClass());
	}

	/**
	 * Registers a new format adapter.
	 *
	 * @param adapter the adapter to register
	 */
	public void registerAdapter (ConfigFormatAdapter adapter) {
		adapters.put(adapter.extension(), adapter);
	}

	private String getExtension (Path file) {
		String name = file.getFileName().toString();
		int dot = name.lastIndexOf('.');
		return dot < 0 ? "" : name.substring(dot);
	}
} 