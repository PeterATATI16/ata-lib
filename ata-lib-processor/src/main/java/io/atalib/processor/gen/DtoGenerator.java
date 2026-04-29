package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;
import io.atalib.processor.FieldModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DtoGenerator {

    private DtoGenerator() {}

    public static String generateResponse(EntityModel model) {
        List<FieldModel> fields = model.getResponseFields();
        return build(model.getPackageName(), model.responseDtoName(), fields, true);
    }

    public static String generateRequest(EntityModel model) {
        List<FieldModel> fields = model.getRequestFields();
        return build(model.getPackageName(), model.requestDtoName(), fields, false);
    }

    private static String build(String pkg, String className, List<FieldModel> fields, boolean includeId) {
        Set<String> imports = new LinkedHashSet<>();
        imports.add("lombok.AllArgsConstructor");
        imports.add("lombok.Builder");
        imports.add("lombok.Getter");
        imports.add("lombok.NoArgsConstructor");
        imports.add("lombok.Setter");
        for (FieldModel f : fields) {
            imports.addAll(f.getTypeImports());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        for (String imp : imports) {
            sb.append("import ").append(imp).append(";\n");
        }
        sb.append("\n@Getter\n@Setter\n@NoArgsConstructor\n@AllArgsConstructor\n@Builder\n");
        sb.append("public class ").append(className).append(" {\n\n");

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

        sb.append("}\n");
        return sb.toString();
    }
}
