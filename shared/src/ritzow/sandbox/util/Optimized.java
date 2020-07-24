package ritzow.sandbox.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element has been hand-optimized and
 * may need to be rewritten given further changes
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface Optimized {
	/**
	 * Indicates the method or component that this optimized
	 * code is derived from.
	 */
	String value();
}
