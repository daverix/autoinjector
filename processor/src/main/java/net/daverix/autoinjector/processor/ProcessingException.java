package net.daverix.autoinjector.processor;


import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;

class ProcessingException extends Exception {
    private final Element element;
    private AnnotationMirror annotationMirror;
    private AnnotationValue annotationValue;

    ProcessingException(String message, Element element, AnnotationMirror annotationMirror, AnnotationValue annotationValue) {
        super(message);
        this.element = element;
        this.annotationMirror = annotationMirror;
        this.annotationValue = annotationValue;
    }

    ProcessingException(String message, Element element, AnnotationMirror annotationMirror) {
        super(message);
        this.element = element;
        this.annotationMirror = annotationMirror;
    }

    ProcessingException(String message, Element element) {
        super(message);
        this.element = element;
    }

    Element getElement() {
        return element;
    }

    AnnotationMirror getAnnotationMirror() {
        return annotationMirror;
    }

    AnnotationValue getAnnotationValue() {
        return annotationValue;
    }
}