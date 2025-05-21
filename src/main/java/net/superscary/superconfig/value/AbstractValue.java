package net.superscary.superconfig.value;

/**
 * Abstract base class for configuration values.
 * <p>
 * This class provides a basic implementation of the {@link ConfigValue} interface,
 * handling the storage and management of both current and default values.
 * </p>
 *
 * @param <T> the type of value this configuration value holds
 * @author SuperScary
 * @since 1.0.0
 */
public abstract class AbstractValue<T> implements ConfigValue<T> {
	/**
	 * The current value of this configuration value.
	 */
	private T value;

	/**
	 * The default value for this configuration value.
	 */
	private final T defaultValue;

	/**
	 * Creates a new AbstractValue with the specified default value.
	 * <p>
	 * The current value is initially set to the default value.
	 * </p>
	 *
	 * @param defaultValue the default value for this configuration value
	 */
	protected AbstractValue (T defaultValue) {
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	/**
	 * Gets the current value of this configuration value.
	 *
	 * @return the current value
	 */
	@Override
	public T get () {
		return value;
	}

	/**
	 * Gets the default value for this configuration value.
	 *
	 * @return the default value
	 */
	@Override
	public T getDefault () {
		return defaultValue;
	}

	/**
	 * Sets a new value for this configuration value.
	 *
	 * @param v the new value to set
	 */
	@Override
	public void set (T v) {
		this.value = v;
	}

}
