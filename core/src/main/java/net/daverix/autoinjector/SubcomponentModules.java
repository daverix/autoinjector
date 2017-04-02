package net.daverix.autoinjector;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Used to generate a sub component builder for activities, fragments and services. The method
 * this annotation annotates must only have one parameter which is the class to inject.</p>
 *

 */
@Retention(RetentionPolicy.SOURCE)
@Documented
@Target(ElementType.METHOD)
public @interface SubcomponentModules {
    /**
     * Dagger Modules to use in the generated sub component.
     *
     * @return array of module classes
     */
    Class<?>[] value() default {};
}
