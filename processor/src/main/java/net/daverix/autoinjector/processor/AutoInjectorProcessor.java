package net.daverix.autoinjector.processor;

import com.google.auto.service.AutoService;

import net.daverix.autoinjector.AutoInjector;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import dagger.Module;

import static java.util.stream.Collectors.toList;
import static net.daverix.autoinjector.processor.AnnotationUtils.getClassArrayAnnotationValue;

@AutoService(Processor.class)
@SupportedAnnotationTypes("net.daverix.autoinjector.AutoInjector")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class AutoInjectorProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        Messager messager = processingEnv.getMessager();
        Elements elementUtils = processingEnv.getElementUtils();
        Types typeUtils = processingEnv.getTypeUtils();
        Filer filer = processingEnv.getFiler();

        Set<? extends Element> annotatedElements = roundEnvironment.getElementsAnnotatedWith(AutoInjector.class);
        for (Element element : annotatedElements) {
            try {
                processAutoInjectorElements(messager, elementUtils, typeUtils, element);
            } catch (ProcessingException e) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        e.getLocalizedMessage(),
                        e.getElement(),
                        e.getAnnotationMirror(),
                        e.getAnnotationValue());
            }
        }
        return false;
    }

    private void processAutoInjectorElements(Messager messager,
                                             Elements elementUtils,
                                             Types typeUtils,
                                             Element element) throws ProcessingException {
        if (isAbstract(element)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Abstract classes not supported for AutoInjector",
                    element);
            return;
        }

        TypeMirror elementType = element.asType();
        AutoInjector autoInjector = element.getAnnotation(AutoInjector.class);

        if (!autoInjector.skipInjectionCheck()) {
            if (!isAssignableToDaggerBaseTypes(typeUtils, elementUtils, elementType)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                        element.getSimpleName() + " must extend from either DaggerActivity, " +
                                "DaggerFragment, DaggerService or DaggerIntentService. You can " +
                                "also set AutoInjector.skipInjectionCheck to true if you want to " +
                                "call AndroidInjection.inject manually.", element);
                return;
            }
        }

        TypeElement typeElement = (TypeElement) element;
        List<TypeElement> modules = getModuleTypes(typeElement);

        modules.stream()
                .filter(AutoInjectorProcessor::isMissingModuleAnnotation)
                .forEach(moduleElement -> messager.printMessage(Diagnostic.Kind.ERROR,
                        moduleElement.getSimpleName() + " in AutoInjector.modules " +
                                "not annotated with dagger.Module", element));
    }

    private static boolean isMissingModuleAnnotation(TypeElement moduleElement) {
        return moduleElement.getAnnotation(Module.class) == null;
    }

    private static boolean isAbstract(Element element) {
        return element.getModifiers().contains(Modifier.ABSTRACT);
    }

    private static boolean isAssignableToDaggerBaseTypes(Types types,
                                                         Elements elements,
                                                         TypeMirror type) {
        return Stream.of(elements.getTypeElement("dagger.android.DaggerActivity"))
                .map(Element::asType)
                .anyMatch(baseType -> types.isAssignable(type, baseType));
    }

    private List<TypeElement> getModuleTypes(TypeElement typeElement)
            throws ProcessingException {
        return getClassArrayAnnotationValue(typeElement, AutoInjector.class, "modules")
                .collect(toList());
    }
}
