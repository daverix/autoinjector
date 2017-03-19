package net.daverix.autoinjector;


import dagger.android.DaggerActivity;

@AutoInjector(modules = {MyModule.class})
public class MyActivity extends DaggerActivity {
}
