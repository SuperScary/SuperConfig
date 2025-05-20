package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

/**
 * Wrapper for enum values in a configuration.
 * <p>
 * This class provides a type-safe way to store and retrieve enum values in a configuration.
 * It supports all enum types and provides methods for getting and setting enum values.
 * </p>
 *
 * @param <E> the enum type
 * @author SuperScary
 * @since 1.0.0
 */
public class EnumValue<E extends Enum<E>> extends AbstractValue<E> {

	/**
	 * The enum class type.
	 */
	private final Class<E> enumClass;

	/**
	 * Creates a new EnumValue with the given enum class and default value.
	 *
	 * @param enumClass the class of the enum
	 * @param defaultValue the default value
	 */
	public EnumValue(Class<E> enumClass, E defaultValue) {
		super(defaultValue);
		this.enumClass = enumClass;
	}

	/**
	 * Creates a new EnumValue with the given enum class and no default value.
	 *
	 * @param enumClass the class of the enum
	 */
	public EnumValue(Class<E> enumClass) {
		super(null);
		this.enumClass = enumClass;
	}

	/**
	 * Sets the value using a string representation of the enum constant.
	 * <p>
	 * This method allows setting the value using the enum constant's name.
	 * The name must exactly match one of the enum constants (case-sensitive).
	 * </p>
	 *
	 * @param name the name of the enum constant
	 * @throws IllegalArgumentException if no enum constant exists with the given name
	 */
	public void set(String name) {
		E e = Enum.valueOf(enumClass, name);
		super.set(e);
	}
}
