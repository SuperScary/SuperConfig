package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

/**
 * A wrapper for boolean values.
 * <p>
 * This class extends {@link AbstractValue} and provides a way to store and manage
 * boolean values in a configuration system.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Config(name = "example")
 * public class ExampleConfig {
 *     public BooleanValue myBoolean = new BooleanValue(false);
 * }
 * }
 * </pre>
 */
public class BooleanValue extends AbstractValue<Boolean> {
	/**
	 * Constructor to create a BooleanValue with a default value.
	 *
	 * @param def the default boolean value
	 */
	public BooleanValue (boolean def) {
		super(def);
	}
}