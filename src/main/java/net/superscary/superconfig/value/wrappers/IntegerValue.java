package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

/**
 * A wrapper for integer values in configuration.
 * This class extends {@link AbstractValue} and provides a way to store and manage
 * integer values in a configuration system.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Config(name = "example")
 * public class ExampleConfig {
 *     public IntegerValue myInt = new IntegerValue(42);
 * }
 * }
 * </pre>
 *
 * @author SuperScary
 * @since 1.0.0
 */
public class IntegerValue extends AbstractValue<Integer> {
	/**
	 * Creates a new IntegerValue with the specified default value.
	 *
	 * @param def the default integer value
	 */
	public IntegerValue (int def) {
		super(def);
	}
}
