package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;

public final class ControllerGenerator {

    private ControllerGenerator() {}

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

        return "package " + pkg + ";\n\n"
                + imports + "\n"
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
