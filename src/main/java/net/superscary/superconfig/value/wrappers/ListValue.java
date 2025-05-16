package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A ConfigValue that holds a List of T.
 */
public class ListValue<T> extends AbstractValue<List<T>> {

	/**
	 * @param defaultList the default contents (will be copied internally)
	 */
	public ListValue(List<T> defaultList) {
		super(new ArrayList<>(defaultList));
	}

	@Override
	public List<T> get() {
		// return the live list so callers can mutate it if desired
		return super.get();
	}

	@Override
	public void set(List<T> newValue) {
		// copy to avoid external aliasing
		super.set(new ArrayList<>(newValue));
	}

	/** convenience: add an element */
	public void add(T element) {
		get().add(element);
	}

	/** convenience: remove an element */
	public void remove(T element) {
		get().remove(element);
	}

	/** convenience: clear the list */
	public void clear() {
		get().clear();
	}

	/** convenience: addAll */
	public void addAll(Collection<? extends T> c) {
		get().addAll(c);
	}
}
