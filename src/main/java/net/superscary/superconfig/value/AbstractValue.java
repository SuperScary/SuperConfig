package net.superscary.superconfig.value;

public abstract class AbstractValue<T> implements ConfigValue<T> {
	private T value;
	private final T defaultValue;

	protected AbstractValue (T defaultValue) {
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	@Override
	public T get () {
		return value;
	}

	@Override
	public T getDefault () {
		return defaultValue;
	}

	@Override
	public void set (T v) {
		this.value = v;
	}
}
