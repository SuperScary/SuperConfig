package net.superscary.superconfig.value.wrappers;

import net.superscary.superconfig.value.AbstractValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Wrapper for list values in a configuration.
 * <p>
 * This class provides a type-safe way to store and retrieve list values in a configuration.
 * It supports all list types and provides methods for getting and setting list values.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Config(name = "example")
 * public class ExampleConfig {
 *     public ListValue<String> myList = new ListValue<>(Arrays.asList("item1", "item2"));
 * }
 * }
 * </pre>
 *
 * @param <E> the type of elements in the list
 * @author SuperScary
 * @since 1.0.0
 */
public class ListValue<E> extends AbstractValue<List<E>> {

	/**
	 * Creates a new ListValue with the given default value.
	 *
	 * @param defaultValue the default value
	 */
	public ListValue(List<E> defaultValue) {
		super(defaultValue);
	}

	/**
	 * Creates a new ListValue with no default value.
	 */
	public ListValue() {
		super(null);
	}

	/**
	 * Gets the current list value.
	 * <p>
	 * Returns the live list so callers can modify it directly.
	 * </p>
	 *
	 * @return the current list value
	 */
	@Override
	public List<E> get() {
		return super.get();
	}

	/**
	 * Sets a new list value.
	 * <p>
	 * The new list is copied internally to prevent external modification.
	 * </p>
	 *
	 * @param newValue the new list value
	 */
	@Override
	public void set(List<E> newValue) {
		super.set(new ArrayList<>(newValue));
	}

	/**
	 * Adds an element to the list.
	 *
	 * @param element the element to add
	 */
	public void add(E element) {
		get().add(element);
	}

	/**
	 * Removes an element from the list.
	 *
	 * @param element the element to remove
	 */
	public void remove(E element) {
		get().remove(element);
	}

	/**
	 * Removes all elements from the list.
	 */
	public void clear() {
		get().clear();
	}

	/**
	 * Adds all elements from a collection to the list.
	 *
	 * @param c the collection containing elements to add
	 */
	public void addAll(Collection<? extends E> c) {
		get().addAll(c);
	}
}
