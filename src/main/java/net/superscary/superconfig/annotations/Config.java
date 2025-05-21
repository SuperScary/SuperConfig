package net.superscary.superconfig.annotations;

import net.superscary.superconfig.format.ConfigFormatType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a configuration class.
 * This annotation specifies the name, format, and location of the configuration file.
 *
 * @author SuperScary
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
	/**
	 * The name of the configuration file (without extension).
	 * If not specified, the class name in lowercase will be used.
	 *
	 * @return the configuration file name
	 */
	String value () default "";

	/**
	 * The format type to use for this configuration.
	 * This determines the file extension and how the configuration is serialized.
	 *
	 * @return the configuration format type
	 */
	ConfigFormatType format () default ConfigFormatType.JSON5;

	/**
	 * The directory path where the configuration file should be stored.
	 * This can be an absolute path or a path relative to the working directory.
	 * If not specified, the file will be created in the working directory.
	 *
	 * @return the configuration file directory path
	 */
	String path () default "";
}
