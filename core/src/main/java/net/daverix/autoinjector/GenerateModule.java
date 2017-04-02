package net.daverix.autoinjector;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Annotated on abstract classes to generate a Dagger Module with bind methods derived from
 * the methods in the annotated class. This reduces the boiler plate you hav </p>
 *
 * <p>For example, the following code: </p>
 * <pre>
 *    {@code @GenerateModule}
 *     public abstract class MyActivityInjector {
 *        {@code @SubcomponentModules(MyModule.class)}
 *         abstract void inject(MyActivity activity);
 *     }
 * </pre>
 *
 * <p>Will generate the following Dagger module:</p>
 * <pre>
 *    {@code @Module(subcomponents = MyActivityComponent.class)}
 *     public abstract class MyActivityInjectorModule {
 *        {@code @Binds}
 *        {@code @IntoMap}
 *        {@code @ActivityKey(MyActivity.class}
 *         abstract{@code AndroidInjector.Factory<? extends Activity>}
 *             bindMyActivityInjectorFactory(MyActivityComponent.Builder builder);
 *     }
 * </pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Documented
@Target(ElementType.TYPE)
public @interface GenerateModule {
    /**
     * <p>Set to change the generated Dagger module's name. The default name is the name of the
     * annotated class suffixed with "Module".</p>
     *
     * @return a custom name.
     */
    String name() default "";

    /**
     * Dagger Modules to use to pass on to includes in the generated Dagger module.
     *
     * @return array of module classes
     */
    Class<?>[] includes() default {};
}
