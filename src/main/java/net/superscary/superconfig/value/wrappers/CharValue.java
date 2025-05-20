package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

/**
 * A wrapper for character values in configuration.
 * This class extends {@link AbstractValue} and provides a way to store and manage
 * character values in a configuration system.
 *
 * Example usage:
 * <pre>
 * {@code
 * @Config(name = "example")
 * public class ExampleConfig {
 *     public CharValue myChar = new CharValue('A');
 * }
 * }
 * </pre>
 *
 * @author SuperScary
 * @since 1.0.0
 */
public class CharValue extends AbstractValue<Character> {

	/**
	 * Creates a new CharValue with the specified default value.
	 *
	 * @param def the default character value
	 */
	public CharValue(char def) {
		super(def);
	}

}
