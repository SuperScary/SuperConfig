package net.superscary.superconfig.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark fields that should be ignored by the config manager.
 * It can be used to exclude certain fields from being processed or serialized.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Ignore
 * private String ignoredField;
 * }
 * </pre>
 *
 * @since 1.1.0
 * @author SuperScary
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Ignore {
}
