package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

/**
 * Wrapper for float values in a configuration.
 * <p>
 * This class provides a type-safe way to store and retrieve float values in a configuration.
 * It supports all float values and provides methods for getting and setting float values.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Config(name = "example")
 * public class ExampleConfig {
 *     public FloatValue myFloat = new FloatValue(3.14f);
 * }
 * }
 * </pre>
 *
 * @author SuperScary
 * @since 1.0.0
 */
public class FloatValue extends AbstractValue<Float> {
	/**
	 * Creates a new FloatValue with the given default value.
	 *
	 * @param defaultValue the default value
	 */
	public FloatValue (Float defaultValue) {
		super(defaultValue);
	}

	/**
	 * Creates a new FloatValue with no default value.
	 */
	public FloatValue () {
		super(null);
	}
}

