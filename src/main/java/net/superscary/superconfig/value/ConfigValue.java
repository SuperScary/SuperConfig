package net.superscary.superconfig.value;

import java.util.function.Supplier;

public interface ConfigValue<T> extends Supplier<T> {
	/**
	 * The default value, used if none present on load
	 */
	T getDefault ();

	/**
	 * Set a new value
	 */
	void set (T value);
}
