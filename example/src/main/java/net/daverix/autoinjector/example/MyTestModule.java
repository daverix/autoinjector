package net.daverix.autoinjector.example;

import android.app.Activity;

import dagger.Binds;
import dagger.Module;
import dagger.Subcomponent;
import dagger.android.ActivityKey;
import dagger.android.AndroidInjector;
import dagger.multibindings.IntoMap;

@Module(subcomponents = MyTestModule.MyActivityComponent.class)
public abstract class MyTestModule {
    @Subcomponent
    interface MyActivityComponent extends AndroidInjector<MyActivity> {
        @Subcomponent.Builder
        abstract class Builder extends AndroidInjector.Builder<MyActivity> {

        }
    }

    @Binds @IntoMap @ActivityKey(MyActivity.class)
    abstract AndroidInjector.Factory<? extends Activity> bindMyActivity(MyActivityComponent.Builder builder);
}
