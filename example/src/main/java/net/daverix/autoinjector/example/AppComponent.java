package net.daverix.autoinjector.example;

import android.app.Activity;

import java.util.Map;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;

@Component(modules = {
        AndroidInjectionModule.class,
        MyActivityModule.class
})
public interface AppComponent {
    Map<Class<? extends Activity>, AndroidInjector.Factory<? extends Activity>> getActivities();
}
