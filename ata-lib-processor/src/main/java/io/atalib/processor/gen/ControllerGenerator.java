package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;

import java.util.List;

public final class ControllerGenerator {

    private ControllerGenerator() {}

    /** Appends {@code name = {"A", "B"}} to sb if roles is non-empty. Returns updated first flag. */
    private static boolean appendSecurityAttr(StringBuilder sb, String name, List<String> roles, boolean first) {
        if (roles.isEmpty()) return first;
        if (!first) sb.append(", ");
        sb.append(name).append(" = {");
        for (int i = 0; i < roles.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(roles.get(i)).append("\"");
        }
        sb.append("}");
        return false;
    }

    public static String generate(EntityModel model) {
        String pkg  = model.getGeneratedPackageName();
        String req  = model.requestDtoName();
        String resp = model.responseDtoName();
        String impl = model.serviceImplName();
        String ctrl = model.controllerName();
        String url  = model.getBaseUrl();

        String idType = model.getIdTypeName();
        StringBuilder imports = new StringBuilder();
        imports.append("import io.atalib.annotation.AtaController;\n");
        imports.append("import io.atalib.controller.AbstractGenericController;\n");
        if (model.hasSecurity()) {
            imports.append("import io.atalib.security.SecuredCrud;\n");
        }
        imports.append("import jakarta.validation.Valid;\n");
        imports.append("import org.springframework.http.ResponseEntity;\n");
        imports.append("import org.springframework.web.bind.annotation.DeleteMapping;\n");
        imports.append("import org.springframework.web.bind.annotation.GetMapping;\n");
        imports.append("import org.springframework.web.bind.annotation.PathVariable;\n");
        imports.append("import org.springframework.web.bind.annotation.PutMapping;\n");
        imports.append("import org.springframework.web.bind.annotation.RequestBody;\n");
        for (String imp : model.getIdTypeImports()) {
            imports.append("import ").append(imp).append(";\n");
        }

        StringBuilder securedCrud = new StringBuilder();
        if (model.hasSecurity()) {
            securedCrud.append("@SecuredCrud(");
            boolean first = true;
            first = appendSecurityAttr(securedCrud, "create", model.getSecuredCreate(), first);
            first = appendSecurityAttr(securedCrud, "update", model.getSecuredUpdate(), first);
            first = appendSecurityAttr(securedCrud, "delete", model.getSecuredDelete(), first);
            first = appendSecurityAttr(securedCrud, "read",   model.getSecuredRead(),   first);
            appendSecurityAttr(securedCrud, "list", model.getSecuredList(), first);
            securedCrud.append(")\n");
        }

        return "package " + pkg + ";\n\n"
                + imports + "\n"
                + securedCrud
                + "@AtaController(\"" + url + "\")\n"
                + "public class " + ctrl + " extends AbstractGenericController<" + req + ", " + resp + ", " + idType + "> {\n\n"
                + "    public " + ctrl + "(" + impl + " service) {\n"
                + "        super(service);\n"
                + "    }\n\n"
                + "    @GetMapping(\"/{id}\")\n"
                + "    @Override\n"
                + "    public ResponseEntity<" + resp + "> getById(@PathVariable " + idType + " id) {\n"
                + "        return super.getById(id);\n"
                + "    }\n\n"
                + "    @PutMapping(\"/{id}\")\n"
                + "    @Override\n"
                + "    public ResponseEntity<" + resp + "> update(@PathVariable " + idType + " id, @Valid @RequestBody " + req + " requestDto) {\n"
                + "        return super.update(id, requestDto);\n"
                + "    }\n\n"
                + "    @DeleteMapping(\"/{id}\")\n"
                + "    @Override\n"
                + "    public ResponseEntity<Void> delete(@PathVariable " + idType + " id) {\n"
                + "        return super.delete(id);\n"
                + "    }\n"
                + "}\n";
    }
}
