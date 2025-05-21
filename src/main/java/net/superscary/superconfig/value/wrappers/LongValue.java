package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

/**
 * A wrapper for long values in configuration.
 * This class extends {@link AbstractValue} and provides a way to store and manage
 * long values in a configuration system.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Config(name = "example")
 * public class ExampleConfig {
 *     public LongValue myLong = new LongValue(42L);
 * }
 * }
 * </pre>
 *
 * @author SuperScary
 * @since 1.0.0
 */
public class LongValue extends AbstractValue<Long> {
	/**
	 * Creates a new LongValue with the specified default value.
	 *
	 * @param def the default long value
	 */
	public LongValue (Long def) {
		super(def);
	}
}

