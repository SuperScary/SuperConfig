package net.superscary.superconfig.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a configuration container.
 * <p>
 * This annotation is used to specify metadata about a configuration class, such as its name
 * in the configuration file. The name will be converted to lowercase when used in the file.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Config(name = "server_config")
 * public class ServerConfig {
 *     // configuration fields
 * }
 * }
 * </pre>
 *
 * @author SuperScary
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config {
	/**
	 * The name of this configuration section in the configuration file.
	 * <p>
	 * This name will be converted to lowercase when used in the file.
	 * For example, if name is "ServerConfig", it will appear as "serverconfig" in the file.
	 * </p>
	 *
	 * @return the name of the configuration section
	 */
	String name();
}
