package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

public class EnumValue<E extends Enum<E>> extends AbstractValue<E> {

	private final Class<E> enumClass;

	/**
	 * @param enumClass  the enum type, e.g. MyMode.class
	 * @param defaultVal the default enum constant, e.g. MyMode.FOO
	 */
	public EnumValue (Class<E> enumClass, E defaultVal) {
		super(defaultVal);
		this.enumClass = enumClass;
	}

	public EnumValue (E defaultVal) {
		this(defaultVal.getDeclaringClass(), defaultVal);
	}

	/**
	 * helper to turn a string into the enum constant
	 */
	public void set (String name) {
		E e = Enum.valueOf(enumClass, name);
		super.set(e);
	}
}
