package net.superscary.superconfig.core;

import net.superscary.superconfig.annotations.Comment;
import net.superscary.superconfig.annotations.Config;
import net.superscary.superconfig.value.ConfigValue;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Core class for serializing and deserializing configuration objects.
 * <p>
 * This class handles the reflection-based mapping between Java objects and
 * their serialized representations. It supports:
 * - Primitive types and their wrappers
 * - Strings and enums
 * - Collections and arrays
 * - Nested configuration objects
 * - Comments and annotations
 * </p>
 *
 * @author SuperScary
 * @since 2.0.0
 */
public class ConfigSerializer {

	/**
	 * Converts a Java object to a Map representation.
	 * <p>
	 * This method recursively processes the object's fields, handling nested
	 * objects, collections, and primitive types.
	 * </p>
	 *
	 * @param obj the object to convert
	 * @return a Map representing the object's state
	 * @throws IllegalAccessException if a field cannot be accessed
	 */
	public Map<String, Object> toMap (Object obj) throws IllegalAccessException {
		Map<String, Object> map = new LinkedHashMap<>();
		Class<?> cls = obj.getClass();

		// Check if the class itself should be ignored
		if (cls.isAnnotationPresent(net.superscary.superconfig.annotations.Ignore.class)) {
			return map;
		}

		// Add class-level comments if available
		List<String> classComments = getClassComments(cls);
		if (!classComments.isEmpty()) {
			map.put("__class_comments", classComments);
		}

		// First handle all non-static fields
		for (Field field : cls.getDeclaredFields()) {
			if (shouldSkipField(field)) continue;

			field.setAccessible(true);
			String key = field.getName().toLowerCase();
			Object value = field.get(obj);

			// Add field comments if available
			List<String> fieldComments = getFieldComments(field);
			if (!fieldComments.isEmpty()) {
				map.put("__field_comments_" + key, fieldComments);
			}

			if (value == null) {
				map.put(key, null);
			} else if (isConfigClass(value.getClass())) {
				map.put(key, toMap(value));
			} else if (value instanceof Collection<?>) {
				map.put(key, convertCollection((Collection<?>) value));
			} else if (value.getClass().isArray()) {
				map.put(key, convertArray(value));
			} else {
				map.put(key, value);
			}
		}

		// Then handle static inner classes
		for (Class<?> innerClass : cls.getDeclaredClasses()) {
			if (Modifier.isStatic(innerClass.getModifiers()) && isConfigClass(innerClass)) {
				// Skip if the inner class is marked with @Ignore
				if (innerClass.isAnnotationPresent(net.superscary.superconfig.annotations.Ignore.class)) {
					continue;
				}
				try {
					Object innerInstance = innerClass.getDeclaredConstructor().newInstance();
					String innerClassName = innerClass.getSimpleName().toLowerCase();

					// Add inner class comments to the parent map with a special key
					List<String> innerClassComments = getClassComments(innerClass);
					if (!innerClassComments.isEmpty()) {
						map.put("__class_comments_" + innerClassName, innerClassComments);
					}

					map.put(innerClassName, toMap(innerInstance));
				} catch (ReflectiveOperationException e) {
					// Skip if we can't create an instance
					continue;
				}
			}
		}

		return map;
	}

	/**
	 * Converts a Map representation back to a Java object.
	 * <p>
	 * This method recursively processes the map entries, handling nested
	 * objects, collections, and primitive types.
	 * </p>
	 *
	 * @param map the map to convert
	 * @param cls the target class
	 * @param <T> the type of the object to create
	 * @return a new instance of the target class
	 * @throws ReflectiveOperationException if the object cannot be instantiated
	 */
	public <T> T fromMap (Map<String, Object> map, Class<T> cls) throws ReflectiveOperationException {
		T instance;
		if (cls.getDeclaringClass() != null) {
			// For static inner classes, we can instantiate directly
			// For non-static inner classes, we use lazy instantiation
			if (Modifier.isStatic(cls.getModifiers())) {
				instance = cls.getDeclaredConstructor().newInstance();
				populateInstance(instance, map);
			} else {
				instance = createLazyInnerClassInstance(cls.getDeclaringClass(), cls, map);
			}
		} else {
			instance = cls.getDeclaredConstructor().newInstance();
			populateInstance(instance, map);
		}

		return instance;
	}

	private void populateInstance (Object instance, Map<String, Object> map) throws ReflectiveOperationException {
		Class<?> cls = instance.getClass();
		for (Field field : cls.getDeclaredFields()) {
			if (shouldSkipField(field)) continue;

			field.setAccessible(true);
			String key = field.getName().toLowerCase();
			Object value = map.get(key);

			if (value == null) {
				// Skip setting null for primitive types
				if (!field.getType().isPrimitive()) {
					field.set(instance, null);
				}
				continue;
			}

			Class<?> fieldType = field.getType();
			if (isConfigClass(fieldType)) {
				@SuppressWarnings("unchecked")
				Map<String, Object> nestedMap = (Map<String, Object>) value;
				Object nested = fromMap(nestedMap, fieldType);
				field.set(instance, nested);
			} else if (Collection.class.isAssignableFrom(fieldType)) {
				Collection<Object> collection = createCollection(fieldType);
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) value;
				for (Object item : list) {
					if (item instanceof Map<?, ?> && isConfigClass(getCollectionElementType(field))) {
						collection.add(fromMap((Map<String, Object>) item, getCollectionElementType(field)));
					} else {
						collection.add(convertValue(item, getCollectionElementType(field)));
					}
				}
				field.set(instance, collection);
			} else if (fieldType.isArray()) {
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) value;
				Object array = Array.newInstance(fieldType.getComponentType(), list.size());
				for (int i = 0; i < list.size(); i++) {
					Object item = list.get(i);
					if (item instanceof Map<?, ?> && isConfigClass(fieldType.getComponentType())) {
						Array.set(array, i, fromMap((Map<String, Object>) item, fieldType.getComponentType()));
					} else {
						Array.set(array, i, convertValue(item, fieldType.getComponentType()));
					}
				}
				field.set(instance, array);
			} else {
				field.set(instance, convertValue(value, fieldType));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T createLazyInnerClassInstance (Class<?> outerClass, Class<T> innerClass, Map<String, Object> map) {
		// Create a proxy that will instantiate the inner class only when needed
		return (T) java.lang.reflect.Proxy.newProxyInstance(
				innerClass.getClassLoader(),
				new Class<?>[]{innerClass},
				(proxy, method, args) -> {
					// If this is the first access, create the actual instance
					if (method.getName().equals("getClass")) {
						return innerClass;
					}

					// Create the outer instance and inner instance
					Object outerInstance = outerClass.getDeclaredConstructor().newInstance();
					T innerInstance = innerClass.getDeclaredConstructor(outerClass).newInstance(outerInstance);

					// Populate the inner instance with values from the map
					for (Field field : innerClass.getDeclaredFields()) {
						if (shouldSkipField(field)) continue;

						field.setAccessible(true);
						String key = field.getName().toLowerCase();
						Object value = map.get(key);

						if (value == null) {
							field.set(innerInstance, null);
							continue;
						}

						Class<?> fieldType = field.getType();
						if (isConfigClass(fieldType)) {
							@SuppressWarnings("unchecked")
							Map<String, Object> nestedMap = (Map<String, Object>) value;
							Object nested = fromMap(nestedMap, fieldType);
							field.set(innerInstance, nested);
						} else if (Collection.class.isAssignableFrom(fieldType)) {
							Collection<Object> collection = createCollection(fieldType);
							@SuppressWarnings("unchecked")
							List<Object> list = (List<Object>) value;
							for (Object item : list) {
								if (item instanceof Map<?, ?> && isConfigClass(getCollectionElementType(field))) {
									collection.add(fromMap((Map<String, Object>) item, getCollectionElementType(field)));
								} else {
									collection.add(convertValue(item, getCollectionElementType(field)));
								}
							}
							field.set(innerInstance, collection);
						} else if (fieldType.isArray()) {
							@SuppressWarnings("unchecked")
							List<Object> list = (List<Object>) value;
							Object array = Array.newInstance(fieldType.getComponentType(), list.size());
							for (int i = 0; i < list.size(); i++) {
								Object item = list.get(i);
								if (item instanceof Map<?, ?> && isConfigClass(fieldType.getComponentType())) {
									Array.set(array, i, fromMap((Map<String, Object>) item, fieldType.getComponentType()));
								} else {
									Array.set(array, i, convertValue(item, fieldType.getComponentType()));
								}
							}
							field.set(innerInstance, array);
						} else {
							field.set(innerInstance, convertValue(value, fieldType));
						}
					}

					// Forward the method call to the actual instance
					return method.invoke(innerInstance, args);
				}
		);
	}

	private boolean shouldSkipField (Field field) {
		int mods = field.getModifiers();
		// Skip static fields that are not config classes
		if (Modifier.isStatic(mods)) {
			return true;
		}
		return Modifier.isTransient(mods) || field.isSynthetic() || field.isAnnotationPresent(net.superscary.superconfig.annotations.Ignore.class);
	}

	private Object getFieldValue (Field field, Object obj) throws IllegalAccessException {
		Object value = field.get(obj);
		if (value instanceof ConfigValue<?> cv) {
			return cv.get();
		}
		return value;
	}

	private boolean isSimpleType (Object value) {
		return value instanceof Number
				|| value instanceof Boolean
				|| value instanceof String
				|| value instanceof Character
				|| value.getClass().isEnum();
	}

	private List<Object> convertCollection (Collection<?> collection) throws IllegalAccessException {
		List<Object> result = new ArrayList<>();
		for (Object item : collection) {
			if (item == null) {
				result.add(null);
			} else if (isSimpleType(item)) {
				result.add(item);
			} else if (isConfigClass(item.getClass())) {
				result.add(toMap(item));
			} else if (item instanceof Collection<?> coll) {
				result.add(convertCollection(coll));
			} else if (item.getClass().isArray()) {
				result.add(convertArray(item));
			}
		}
		return result;
	}

	private List<Object> convertArray (Object array) throws IllegalAccessException {
		List<Object> result = new ArrayList<>();
		int length = Array.getLength(array);
		for (int i = 0; i < length; i++) {
			Object item = Array.get(array, i);
			if (item == null) {
				result.add(null);
			} else if (isSimpleType(item)) {
				result.add(item);
			} else if (isConfigClass(item.getClass())) {
				result.add(toMap(item));
			} else if (item instanceof Collection<?> coll) {
				result.add(convertCollection(coll));
			} else if (item.getClass().isArray()) {
				result.add(convertArray(item));
			}
		}
		return result;
	}

	private Collection<Object> createCollection (Class<?> type) {
		if (List.class.isAssignableFrom(type)) {
			return new ArrayList<>();
		} else if (Set.class.isAssignableFrom(type)) {
			return new HashSet<>();
		} else if (Queue.class.isAssignableFrom(type)) {
			return new LinkedList<>();
		}
		return new ArrayList<>();
	}

	private Class<?> getCollectionElementType (Field field) {
		String typeName = field.getGenericType().getTypeName();
		if (typeName.contains("<")) {
			String elementTypeName = typeName.substring(typeName.indexOf('<') + 1, typeName.lastIndexOf('>'));
			try {
				// Handle inner classes
				if (elementTypeName.contains("$")) {
					String[] parts = elementTypeName.split("\\$");
					Class<?> outerClass = Class.forName(parts[0]);
					for (Class<?> innerClass : outerClass.getDeclaredClasses()) {
						if (innerClass.getSimpleName().equals(parts[1])) {
							return innerClass;
						}
					}
				}
				return Class.forName(elementTypeName);
			} catch (ClassNotFoundException e) {
				// If class not found, return Object.class
				return Object.class;
			}
		}
		return Object.class;
	}

	private boolean isConfigClass (Class<?> cls) {
		return cls.isAnnotationPresent(Config.class) || cls.getDeclaringClass() != null;
	}

	private Object convertValue (Object value, Class<?> targetType) {
		if (value == null) return null;
		if (targetType.isInstance(value)) return value;
		if (targetType.isEnum()) {
			return Enum.valueOf((Class<? extends Enum>) targetType, value.toString());
		}
		if (targetType == String.class) return value.toString();
		if (targetType == Integer.class || targetType == int.class) return Integer.parseInt(value.toString());
		if (targetType == Long.class || targetType == long.class) return Long.parseLong(value.toString());
		if (targetType == Double.class || targetType == double.class) return Double.parseDouble(value.toString());
		if (targetType == Float.class || targetType == float.class) return Float.parseFloat(value.toString());
		if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(value.toString());
		if (targetType == Character.class || targetType == char.class) return value.toString().charAt(0);
		if (targetType == Byte.class || targetType == byte.class) return Byte.parseByte(value.toString());
		if (targetType == Short.class || targetType == short.class) return Short.parseShort(value.toString());
		if (isConfigClass(targetType)) {
			try {
				return fromMap((Map<String, Object>) value, targetType);
			} catch (ReflectiveOperationException e) {
				throw new IllegalArgumentException("Failed to convert value to " + targetType.getName(), e);
			}
		}
		throw new IllegalArgumentException("Cannot convert " + value + " to " + targetType);
	}

	private List<String> getClassComments (Class<?> cls) {
		Comment comment = cls.getAnnotation(Comment.class);
		if (comment != null) {
			return Arrays.asList(comment.value());
		}
		return Collections.emptyList();
	}

	private List<String> getFieldComments (Field field) {
		Comment comment = field.getAnnotation(Comment.class);
		if (comment != null) {
			return Arrays.asList(comment.value());
		}
		return Collections.emptyList();
	}
} 