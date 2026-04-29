package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;

public final class ControllerGenerator {

    private ControllerGenerator() {}

    public static String generate(EntityModel model) {
        String pkg    = model.getPackageName();
        String req    = model.requestDtoName();
        String resp   = model.responseDtoName();
        String impl   = model.serviceImplName();
        String ctrl   = model.controllerName();
        String url    = model.getBaseUrl();

        return "package " + pkg + ";\n\n"
                + "import io.atalib.annotation.AtaController;\n"
                + "import io.atalib.controller.AbstractGenericController;\n\n"
                + "@AtaController(\"" + url + "\")\n"
                + "public class " + ctrl
                + " extends AbstractGenericController<" + req + ", " + resp + ", Long> {\n\n"
                + "    public " + ctrl + "(" + impl + " service) {\n"
                + "        super(service);\n"
                + "    }\n"
                + "}\n";
    }
}
