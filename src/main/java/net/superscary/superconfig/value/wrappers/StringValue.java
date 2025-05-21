package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

/**
 * A wrapper for string values in configuration.
 * This class extends {@link AbstractValue} and provides a way to store and manage
 * string values in a configuration system.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Config(name = "example")
 * public class ExampleConfig {
 *     public StringValue myString = new StringValue("default");
 * }
 * }
 * </pre>
 *
 * @author SuperScary
 * @since 1.0.0
 */
public class StringValue extends AbstractValue<String> {
	/**
	 * Creates a new StringValue with the specified default value.
	 *
	 * @param def the default string value
	 */
	public StringValue (String def) {
		super(def);
	}
}
