package net.superscary.superconfig.format;

import net.superscary.superconfig.format.formats.*;

/**
 * Factory class for creating configuration format adapters.
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class ConfigFormatFactory {
	/**
	 * Creates a new configuration format adapter for the specified format type.
	 *
	 * @param type the format type
	 * @return a new configuration format adapter
	 */
	public static ConfigFormatAdapter createAdapter (ConfigFormatType type) {
		return switch (type) {
			case JSON -> new JsonFormatAdapter();
			case JSON5 -> new Json5FormatAdapter();
			case TOML -> new TomlFormatAdapter();
			case YAML -> new YamlFormatAdapter();
			case XML -> new XmlFormatAdapter();
		};
	}
} 