package net.daverix.autoinjector.processor;

import com.google.auto.service.AutoService;

import net.daverix.autoinjector.GenerateModule;
import net.daverix.autoinjector.IgnoreBaseClassCheck;
import net.daverix.autoinjector.SubcomponentModules;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import dagger.Module;

import static com.google.auto.common.AnnotationMirrors.getAnnotationValue;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static net.daverix.autoinjector.processor.AnnotationUtils.getAnnotationMirror;
import static net.daverix.autoinjector.processor.AnnotationUtils.getClassArrayAnnotationValue;

@AutoService(Processor.class)
@SupportedAnnotationTypes("net.daverix.autoinjector.GenerateModule")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class GenerateModuleProcessor extends AbstractProcessor {
    private Messager messager;
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        messager = processingEnv.getMessager();
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        Set<? extends Element> annotatedElements = roundEnvironment.getElementsAnnotatedWith(GenerateModule.class);
        for (Element element : annotatedElements) {
            try {
                processGenerateModuleElement(element);
            } catch (Exception e) {
                String stacktrace = getStacktrace(e);

                messager.printMessage(Diagnostic.Kind.ERROR,
                        e.getLocalizedMessage() + ":\n" + stacktrace,
                        element);
            }
        }
        return false;
    }

    private void processGenerateModuleElement(Element element) {
        if(!isAbstract(element)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Classes annotated with GenerateModule must be abstract",
                    element);
            return;
        }

        TypeElement typeElement = (TypeElement) element;
        Optional<? extends AnnotationMirror> annotationMirror = getAnnotationMirror(typeElement, GenerateModule.class);
        if (!annotationMirror.isPresent()) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Cannot find annotation class GenerateModule",
                    element);
            return;
        }

        processGenerateModuleElementWithMirror(typeElement, annotationMirror.get());
    }

    private static String getStacktrace(Exception e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.getBuffer().toString();
    }

    private void processGenerateModuleElementWithMirror(TypeElement element,
                                                        AnnotationMirror annotationMirror) {
        AnnotationValue includesValue = getAnnotationValue(annotationMirror, "includes");
        List<TypeElement> includes = getClassArrayAnnotationValue(includesValue);

        boolean foundErrors = false;
        for (TypeElement include : includes) {
            if(isMissingModuleAnnotation(include)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        String.format("%s in includes not annotated with dagger.Module",
                                include.getSimpleName()), element, annotationMirror, includesValue);
                foundErrors = true;
            }
        }

        if (foundErrors) return;

        List<ExecutableElement> methods = getMethods(element);
        for (ExecutableElement method : methods) {
            if (method.getReturnType().getKind() != TypeKind.VOID) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "must return void", method);
                foundErrors = true;
            }

            if (method.getParameters().size() != 1) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        "GenerateModule methods must have one parameter", method);
                foundErrors = true;
            }
        }
        if(foundErrors) return;

        List<Subcomponent> subcomponents = new ArrayList<>();
        for (ExecutableElement method : methods) {
            List<? extends VariableElement> parameters = method.getParameters();
            VariableElement injectElement = parameters.get(0);
            TypeElement injectTypeElement = (TypeElement) typeUtils.asElement(injectElement.asType());

            if (!isIgnoringBaseClassCheck(injectTypeElement) &&
                    !isAssignableToDaggerBaseTypes(injectElement.asType())) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        injectTypeElement.getSimpleName() + " must extend from either DaggerActivity, " +
                                "DaggerFragment, DaggerService or DaggerIntentService. You can " +
                                "also annotate the method with @IgnoreBaseClassCheck and " +
                                "call AndroidInjection.inject manually.", element);
                return;
            }

            List<TypeElement> componentModuleTypes = getSubcomponentModules(method);

            //TODO: check base type
            subcomponents.add(new ActivitySubcomponent(
                    injectTypeElement.getQualifiedName().toString(),
                    injectTypeElement.getSimpleName().toString(),
                    componentModuleTypes.stream().map(type -> type.getSimpleName().toString()).collect(toList()),
                    componentModuleTypes.stream().map(type -> type.getQualifiedName().toString()).collect(toList())));
        }

        AnnotationValue value = getAnnotationValue(annotationMirror, "name");
        String moduleName = (String) value.getValue();
        if(moduleName.isEmpty()) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Name cannot be empty",
                    element,
                    annotationMirror,
                    value);
            return;
        }

        writeDaggerModule(element,
                moduleName,
                includes,
                subcomponents);
    }

    private List<TypeElement> getSubcomponentModules(ExecutableElement method) {
        Optional<? extends AnnotationMirror> subComponentAnnotationMirror = getAnnotationMirror(method, SubcomponentModules.class);
        if(!subComponentAnnotationMirror.isPresent())
            return new ArrayList<>();

        AnnotationValue subComponentModulesValue = getAnnotationValue(subComponentAnnotationMirror.get(), "value");
        return getClassArrayAnnotationValue(subComponentModulesValue);
    }

    private void writeDaggerModule(TypeElement element,
                                   String moduleName,
                                   List<TypeElement> includes,
                                   List<Subcomponent> subcomponents) {
        String packageName = getPackageName(element);

        Set<String> uniqueImports = new HashSet<>();
        for (Subcomponent subcomponent : subcomponents) {
            uniqueImports.addAll(subcomponent.getBindMethodImports());
        }
        uniqueImports.add("dagger.Module");
        uniqueImports.addAll(includes.stream()
                .map(type -> type.getQualifiedName().toString())
                .collect(toList()));

        for (Subcomponent subcomponent : subcomponents) {
            writeSubComponent(element, packageName, subcomponent);
        }

        JavaFileObject jfo;
        try {
            jfo = filer.createSourceFile(packageName + "." + moduleName);
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Can't create file for dagger module:\n" + getStacktrace(e), element);
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(jfo.openWriter())) {
            bw.write("package " + packageName + ";\n");
            bw.write("\n");

            for (String importName : uniqueImports) {
                bw.write("import " + importName + ";\n");
            }
            bw.write("\n");

            bw.write("@Module(subcomponents = ");
            if(subcomponents.size() == 1) {
                bw.write(getSubcomponentAttribute(subcomponents.get(0)));
            } else {
                bw.write(getSubcomponentAttributes(subcomponents));
            }
            bw.write(")\n");
            bw.write("abstract class " + moduleName + " {\n");

            for (Subcomponent subcomponent : subcomponents) {
                subcomponent.writeBindMethod(bw);
            }

            bw.write("}\n");
            bw.flush();
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Can't write file for dagger module:\n" + getStacktrace(e), element);
        }
    }

    private void writeSubComponent(Element moduleElement, String packageName, Subcomponent subcomponent) {
        JavaFileObject jfo;
        try {
            jfo = filer.createSourceFile(packageName + "." + subcomponent.getSimpleName());
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Can't create file for dagger sub component:\n" + getStacktrace(e), moduleElement);
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(jfo.openWriter())) {
            bw.write("package " + packageName + ";\n");
            bw.write("\n");

            for (String importName : subcomponent.getComponentImports()) {
                bw.write("import " + importName + ";\n");
            }
            bw.write("\n");

            subcomponent.writeComponent(bw);
            bw.flush();
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Can't write file for dagger sub component:\n" + getStacktrace(e), moduleElement);
        }
    }

    private String getSubcomponentAttribute(Subcomponent subcomponent) {
        return String.format("%s.class", subcomponent.getSimpleName());
    }

    private String getSubcomponentAttributes(List<Subcomponent> subcomponents) {
        return String.format("{\n    %s\n}", subcomponents.stream()
                .map(this::getSubcomponentAttribute)
                .collect(joining(",\n    ")));
    }

    private String getPackageName(TypeElement element) {
        return elementUtils.getPackageOf(element).getQualifiedName().toString();
    }

    private boolean isIgnoringBaseClassCheck(TypeElement injectTypeElement) {
        return injectTypeElement.getAnnotation(IgnoreBaseClassCheck.class) != null;
    }

    private List<ExecutableElement> getMethods(Element element) {
        return element.getEnclosedElements().stream()
                .filter(elem -> elem.getKind() == ElementKind.METHOD)
                .map(elem -> (ExecutableElement) elem)
                .collect(toList());
    }

    private static boolean isAbstract(Element element) {
        return element.getModifiers().contains(Modifier.ABSTRACT);
    }

    private static boolean isMissingModuleAnnotation(TypeElement moduleElement) {
        return moduleElement.getAnnotation(Module.class) == null;
    }

    private boolean isAssignableToDaggerBaseTypes(TypeMirror type) {
        return Stream.of(elementUtils.getTypeElement("dagger.android.DaggerActivity"))
                .map(Element::asType)
                .anyMatch(baseType -> typeUtils.isAssignable(type, baseType));
    }
}
