package net.superscary.superconfig.value;

import java.util.function.Supplier;

/**
 * Interface for configuration values that can be stored and retrieved.
 * <p>
 * This interface extends {@link java.util.function.Supplier} to provide a way to get the current value
 * and adds methods for managing default values and value updates.
 * </p>
 *
 * @param <T> the type of value this configuration value holds
 * @author SuperScary
 * @since 1.0.0
 */
public interface ConfigValue<T> extends Supplier<T> {
	/**
	 * Gets the default value for this configuration value.
	 * <p>
	 * The default value is used when no value is present during configuration loading
	 * or when the configuration needs to be reset to its initial state.
	 * </p>
	 *
	 * @return the default value for this configuration value
	 */
	T getDefault();

	/**
	 * Sets a new value for this configuration value.
	 * <p>
	 * This method updates the current value of the configuration. The value should be
	 * of the same type as specified by the generic parameter T.
	 * </p>
	 *
	 * @param value the new value to set
	 */
	void set(T value);
}
