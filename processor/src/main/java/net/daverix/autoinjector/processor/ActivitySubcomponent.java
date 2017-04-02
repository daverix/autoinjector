package net.daverix.autoinjector.processor;


import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

class ActivitySubcomponent implements Subcomponent {
    private final String activityQualifiedName;
    private final String activitySimpleName;
    private final String simpleName;

    public ActivitySubcomponent(String activityQualifiedName,
                                String activitySimpleName) {
        this.activityQualifiedName = activityQualifiedName;
        this.activitySimpleName = activitySimpleName;
        this.simpleName = activitySimpleName + "Component";
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public List<String> getImportNames() {
        return Arrays.asList(activityQualifiedName,
                "android.app.Activity",
                "dagger.Binds",
                "dagger.Module",
                "dagger.Subcomponent",
                "dagger.android.ActivityKey",
                "dagger.android.AndroidInjector",
                "dagger.multibindings.IntoMap");
    }

    @Override
    public void writeComponent(Writer writer) throws IOException {
        writer.write("    @Subcomponent\n");
        writer.write("    public interface " + getSimpleName() + " extends AndroidInjector<" + activitySimpleName + "> {\n");
        writer.write("        @Subcomponent.Builder\n");
        writer.write("        public abstract class Builder extends AndroidInjector.Builder<" + activitySimpleName + "> { }\n");
        writer.write("    }\n");
    }

    @Override
    public void writeBindMethod(Writer writer) throws IOException {
        writer.write("    @Binds @IntoMap @ActivityKey(" + activitySimpleName + ".class)\n");
        writer.write("    public abstract AndroidInjector.Factory<? extends Activity> bind" + activitySimpleName + "(" + simpleName + ".Builder builder);\n");
        writer.write("\n");
    }
}
