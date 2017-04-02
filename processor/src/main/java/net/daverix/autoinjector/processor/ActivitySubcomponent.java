package net.daverix.autoinjector.processor;


import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;

class ActivitySubcomponent implements Subcomponent {
    private final String activityQualifiedName;
    private final String activitySimpleName;
    private final String simpleName;
    private final List<String> simpleModuleNames;
    private final List<String> qualifiedModuleNames;

    public ActivitySubcomponent(String activityQualifiedName,
                                String activitySimpleName,
                                List<String> simpleModuleNames,
                                List<String> qualifiedModuleNames) {
        this.activityQualifiedName = activityQualifiedName;
        this.activitySimpleName = activitySimpleName;
        this.simpleName = activitySimpleName + "Component";
        this.simpleModuleNames = simpleModuleNames;
        this.qualifiedModuleNames = qualifiedModuleNames;
    }

    @Override
    public String getSimpleName() {
        return simpleName;
    }

    @Override
    public List<String> getComponentImports() {
        List<String> imports = new ArrayList<>(Arrays.asList(activityQualifiedName,
                "dagger.Subcomponent",
                "dagger.android.AndroidInjector"));

        imports.addAll(qualifiedModuleNames);
        return imports;
    }

    @Override
    public List<String> getBindMethodImports() {
        return Arrays.asList(activityQualifiedName,
                "android.app.Activity",
                "dagger.Binds",
                "dagger.android.ActivityKey",
                "dagger.android.AndroidInjector",
                "dagger.multibindings.IntoMap");
    }

    @Override
    public void writeComponent(Writer writer) throws IOException {
        writer.write("@Subcomponent");
        if(simpleModuleNames.size() > 0) {
            writer.write("(modules = ");
        }

        if(simpleModuleNames.size() == 1) {
            writer.write(simpleModuleNames.get(0) + ".class");
        } else if(simpleModuleNames.size() > 1) {
            writer.write("{\n    " + simpleModuleNames.stream().collect(joining("\n,    ")) + "\n}");
        }

        if(simpleModuleNames.size() > 0) {
            writer.write(")");
        }

        writer.write("\n");
        writer.write("public interface " + getSimpleName() + " extends AndroidInjector<" + activitySimpleName + "> {\n");
        writer.write("    @Subcomponent.Builder\n");
        writer.write("    public abstract class Builder extends AndroidInjector.Builder<" + activitySimpleName + "> { }\n");
        writer.write("}\n");
    }

    @Override
    public void writeBindMethod(Writer writer) throws IOException {
        writer.write("    @Binds @IntoMap @ActivityKey(" + activitySimpleName + ".class)\n");
        writer.write("    abstract AndroidInjector.Factory<? extends Activity> bind" + activitySimpleName + "(" + simpleName + ".Builder builder);\n");
        writer.write("\n");
    }
}
