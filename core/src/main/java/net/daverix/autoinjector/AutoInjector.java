package net.daverix.autoinjector;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Annotated on activities, fragments and services in order to generate a Dagger module and a sub
 * component for each annotated class.</p>
 *
 * <p>For example, the following code: </p>
 * <pre>
 *    {@code @AutoInjector(modules = MyModule.class}
 *     public class MyActivity extends DaggerActivity {
 *
 *     }
 * </pre>
 *
 * <p>Will generate the following sub component and module</p>
 * <pre>
 *    {@code @SubComponent(modules = MyModule.class)}
 *     public interface MyActivityComponent extends{@code AndroidInjector<MyActivity>} {
 *        {@code @SubComponent.Builder}
 *         abstract class Builder extends{@code AndroidInjector.Builder<MyActivity>} { }
 *     }
 *
 *    {@code @Module(subcomponents = MyActivityComponent.class)}
 *     public abstract class MyActivityModule {
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
public @interface AutoInjector {
    /**
     * Dagger modules to use for the generated sub component.
     *
     * @return array of module classes
     */
    Class<?>[] modules() default {};

    /**
     * The processor checks by default if the annotated class is extending any of the Dagger
     * provided base classes for Android. Set this to false if you area calling
     * AndroidInjection.inject yourself in any of Android's base classes.
     *
     * @return true if check of injection should be skipped, false otherwise
     */
    boolean skipInjectionCheck() default false;

    /**
     * <p>Set to change the generated Dagger module's name. The generated name is the name of the
     * annotated class suffixed with "Module".</p>
     *
     * @return a custom name.
     */
    String moduleName() default "";

    /**
     * <p>Set to change the generated Dagger component's name. The generated name is the name of
     * the annotated class suffixed with "Component".</p>
     *
     * @return a custom name.
     */
    String componentName() default "";
}
