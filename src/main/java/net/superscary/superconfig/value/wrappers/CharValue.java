package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;
import net.superscary.superconfig.value.ConfigValue;

/**
 * A wrapper for a char value.
 *
 * @see AbstractValue
 * @see ConfigValue
 * @since 1.1.0
 * @version 1.1.0
 * @author SuperScary
 */
public class CharValue extends AbstractValue<Character> {

	public CharValue (char def) {
		super(def);
	}

}
