package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

/**
 * A wrapper for byte values in configuration.
 * This class extends {@link AbstractValue} and provides a way to store and manage
 * byte values in a configuration system. It is particularly useful for storing
 * small integer values that need to be constrained to the byte range (-128 to 127).
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Config(name = "example")
 * public class ExampleConfig {
 *     public ByteValue myByte = new ByteValue((byte) 42);
 * }
 * }
 * </pre>
 *
 * @author SuperScary
 * @since 1.0.0
 */
public class ByteValue extends AbstractValue<Byte> {
	/**
	 * Creates a new ByteValue with the specified default value.
	 * The value should be within the valid byte range (-128 to 127).
	 *
	 * @param def the default byte value
	 */
	public ByteValue (Byte def) {
		super(def);
	}
}

