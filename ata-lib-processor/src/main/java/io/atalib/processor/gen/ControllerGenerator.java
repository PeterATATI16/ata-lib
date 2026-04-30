package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;

public final class ControllerGenerator {

    private ControllerGenerator() {}

    public static String generate(EntityModel model) {
        String pkg  = model.getPackageName();
        String req  = model.requestDtoName();
        String resp = model.responseDtoName();
        String impl = model.serviceImplName();
        String ctrl = model.controllerName();
        String url  = model.getBaseUrl();

        return "package " + pkg + ";\n\n"
                + "import io.atalib.annotation.AtaController;\n"
                + "import io.atalib.controller.AbstractGenericController;\n"
                + "import jakarta.validation.Valid;\n"
                + "import org.springframework.http.ResponseEntity;\n"
                + "import org.springframework.web.bind.annotation.DeleteMapping;\n"
                + "import org.springframework.web.bind.annotation.GetMapping;\n"
                + "import org.springframework.web.bind.annotation.PathVariable;\n"
                + "import org.springframework.web.bind.annotation.PutMapping;\n"
                + "import org.springframework.web.bind.annotation.RequestBody;\n\n"
                + "@AtaController(\"" + url + "\")\n"
                + "public class " + ctrl + " extends AbstractGenericController<" + req + ", " + resp + ", Long> {\n\n"
                + "    public " + ctrl + "(" + impl + " service) {\n"
                + "        super(service);\n"
                + "    }\n\n"
                + "    @GetMapping(\"/{id}\")\n"
                + "    @Override\n"
                + "    public ResponseEntity<" + resp + "> getById(@PathVariable Long id) {\n"
                + "        return super.getById(id);\n"
                + "    }\n\n"
                + "    @PutMapping(\"/{id}\")\n"
                + "    @Override\n"
                + "    public ResponseEntity<" + resp + "> update(@PathVariable Long id, @Valid @RequestBody " + req + " requestDto) {\n"
                + "        return super.update(id, requestDto);\n"
                + "    }\n\n"
                + "    @DeleteMapping(\"/{id}\")\n"
                + "    @Override\n"
                + "    public ResponseEntity<Void> delete(@PathVariable Long id) {\n"
                + "        return super.delete(id);\n"
                + "    }\n"
                + "}\n";
    }
}
