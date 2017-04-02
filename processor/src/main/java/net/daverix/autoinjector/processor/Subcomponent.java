package net.daverix.autoinjector.processor;


import java.io.IOException;
import java.io.Writer;
import java.util.List;

interface Subcomponent {
    String getSimpleName();
    List<String> getImportNames();

    void writeComponent(Writer writer) throws IOException;

    void writeBindMethod(Writer writer) throws IOException;
}
