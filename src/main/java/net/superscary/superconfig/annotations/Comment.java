package net.superscary.superconfig.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to add comments to configuration fields.
 * This annotation allows you to add comments to configuration fields, which will be
 * written to the configuration file.
 *
 * @author SuperScary
 * @version 2.0.0
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Comment {
	/**
	 * Lines to emit above the property.
	 * Each line will be written as a comment in the configuration file.
	 *
	 * @return array of comment lines to be written above the property
	 */
	String[] value ();
}