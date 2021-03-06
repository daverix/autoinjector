package net.daverix.autoinjector.processor;

import com.google.auto.common.AnnotationMirrors;
import com.google.auto.common.MoreTypes;
import com.google.common.collect.ImmutableList;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;

final class AnnotationUtils {
    private static final AnnotationValueVisitor<TypeMirror, Void> AS_TYPE =
            new SimpleAnnotationValueVisitor8<TypeMirror, Void>() {
                @Override
                public TypeMirror visitType(TypeMirror t, Void p) {
                    return t;
                }

                @Override
                protected TypeMirror defaultAction(Object o, Void p) {
                    throw new TypeNotPresentException(o.toString(), null);
                }
            };

    private static final AnnotationValueVisitor<ImmutableList<AnnotationValue>, String> AS_ANNOTATION_VALUES =
            new SimpleAnnotationValueVisitor8<ImmutableList<AnnotationValue>, String>() {
                @Override
                public ImmutableList<AnnotationValue> visitArray(
                        List<? extends AnnotationValue> vals, String elementName) {
                    return ImmutableList.copyOf(vals);
                }

                @Override
                protected ImmutableList<AnnotationValue> defaultAction(Object o, String elementName) {
                    throw new IllegalArgumentException(elementName + " is not an array: " + o);
                }
            };

    private static AnnotationValueVisitor<ImmutableList<AnnotationValue>, String> asAnnotationValues() {
        return AS_ANNOTATION_VALUES;
    }

    private static TypeMirror asType(AnnotationValue annotationValue) {
        return AS_TYPE.visit(annotationValue);
    }

    private static Optional<? extends AnnotationMirror> getAnnotationMirror(TypeElement typeElement, Class<?> clazz) {
        String clazzName = clazz.getName();
        return typeElement.getAnnotationMirrors()
                .stream()
                .filter(mirror -> mirror.getAnnotationType().toString().equals(clazzName))
                .findFirst();
    }

    static Stream<TypeElement> getClassArrayAnnotationValue(TypeElement typeElement,
                                                            Class<? extends Annotation> annotationClass,
                                                            String attributeName) throws ProcessingException {
        Optional<? extends AnnotationMirror> configAnnotationMirror = getAnnotationMirror(typeElement, annotationClass);
        if (!configAnnotationMirror.isPresent()) {
            throw new ProcessingException("Cannot find annotation class " + annotationClass, typeElement);
        }

        AnnotationMirror mirror = configAnnotationMirror.get();
        return AnnotationMirrors.getAnnotationValue(mirror, attributeName)
                .accept(asAnnotationValues(), null)
                .stream()
                .map(AnnotationUtils::asType)
                .map(MoreTypes::asTypeElement);
    }
}
