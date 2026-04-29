package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;
import io.atalib.processor.FieldModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DtoGenerator {

    private DtoGenerator() {}

    public static String generateResponse(EntityModel model) {
        return build(model.getPackageName(), model.responseDtoName(), model.getResponseFields(), true);
    }

    public static String generateRequest(EntityModel model) {
        return build(model.getPackageName(), model.requestDtoName(), model.getRequestFields(), false);
    }

    private static String build(String pkg, String className, List<FieldModel> fields, boolean includeId) {
        Set<String> imports = new LinkedHashSet<>();
        for (FieldModel f : fields) {
            imports.addAll(f.getTypeImports());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        if (!imports.isEmpty()) sb.append("\n");
        sb.append("public class ").append(className).append(" {\n\n");

        // Fields
        if (includeId) {
            sb.append("    private Long id;\n");
        }
        for (FieldModel f : fields) {
            sb.append("\n");
            for (String ann : f.getAnnotations()) {
                sb.append("    ").append(ann).append("\n");
            }
            sb.append("    private ").append(f.getTypeDisplayName()).append(" ").append(f.getName()).append(";\n");
        }

        // No-args constructor
        sb.append("\n    public ").append(className).append("() {}\n");

        // All-args constructor
        sb.append("\n    public ").append(className).append("(");
        boolean firstParam = true;
        if (includeId) {
            sb.append("Long id");
            firstParam = false;
        }
        for (FieldModel f : fields) {
            if (!firstParam) sb.append(", ");
            sb.append(f.getTypeDisplayName()).append(" ").append(f.getName());
            firstParam = false;
        }
        sb.append(") {\n");
        if (includeId) sb.append("        this.id = id;\n");
        for (FieldModel f : fields) {
            sb.append("        this.").append(f.getName()).append(" = ").append(f.getName()).append(";\n");
        }
        sb.append("    }\n");

        // Explicit getters and setters (avoid Lombok ordering issues with MapStruct multi-round APT)
        if (includeId) {
            sb.append("\n    public Long getId() { return id; }\n");
            sb.append("    public void setId(Long id) { this.id = id; }\n");
        }
        for (FieldModel f : fields) {
            String cap = capitalize(f.getName());
            sb.append("\n    public ").append(f.getTypeDisplayName())
                    .append(" get").append(cap).append("() { return ").append(f.getName()).append("; }\n");
            sb.append("    public void set").append(cap).append("(")
                    .append(f.getTypeDisplayName()).append(" ").append(f.getName())
                    .append(") { this.").append(f.getName()).append(" = ").append(f.getName()).append("; }\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
