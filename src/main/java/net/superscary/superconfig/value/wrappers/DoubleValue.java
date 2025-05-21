package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

/**
 * A wrapper for double values in configuration.
 * This class extends {@link AbstractValue} and provides a way to store and manage
 * double values in a configuration system.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Config(name = "example")
 * public class ExampleConfig {
 *     public DoubleValue myDouble = new DoubleValue(3.14);
 * }
 * }
 * </pre>
 *
 * @author SuperScary
 * @since 1.0.0
 */
public class DoubleValue extends AbstractValue<Double> {
	/**
	 * Creates a new DoubleValue with the specified default value.
	 *
	 * @param def the default double value
	 */
	public DoubleValue (Double def) {
		super(def);
	}
}
