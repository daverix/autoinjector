# autoinjector
Generates boilerplate code for Dagger Android

## Deprecated
The same functionality has been added to Dagger 2.11+ by using the following following syntax:

```
@Module
class Module {
    @ContributesAndroidInjector
    MyActivity contributesInjectorForMyActivity();
}
```
