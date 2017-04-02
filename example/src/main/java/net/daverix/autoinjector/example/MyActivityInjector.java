package net.daverix.autoinjector.example;

import net.daverix.autoinjector.GenerateModule;
import net.daverix.autoinjector.SubcomponentModules;

@GenerateModule(name = "MyActivityModule")
abstract class MyActivityInjector {
    @SubcomponentModules(value = MyModule.class)
    abstract void injectMyActivity(MyActivity activity);
}
