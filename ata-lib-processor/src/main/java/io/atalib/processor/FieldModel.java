package io.atalib.processor;

import java.util.List;

public class FieldModel {

    private final String name;
    private final String typeDisplayName;
    private final List<String> typeImports;
    private final List<String> annotations;

    public FieldModel(String name, String typeDisplayName, List<String> typeImports, List<String> annotations) {
        this.name = name;
        this.typeDisplayName = typeDisplayName;
        this.typeImports = typeImports;
        this.annotations = annotations;
    }

    public String getName() { return name; }
    public String getTypeDisplayName() { return typeDisplayName; }
    public List<String> getTypeImports() { return typeImports; }
    public List<String> getAnnotations() { return annotations; }
}
