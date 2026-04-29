package io.atalib.processor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TypeUtils {

    private TypeUtils() {}

    public static class TypeResolution {
        public final String displayName;
        public final Set<String> imports;

        TypeResolution(String displayName, Set<String> imports) {
            this.displayName = displayName;
            this.imports = imports;
        }
    }

    public static TypeResolution resolve(String fqn) {
        Set<String> imports = new LinkedHashSet<>();
        String displayName = simplify(fqn, imports);
        return new TypeResolution(displayName, imports);
    }

    private static String simplify(String fqn, Set<String> imports) {
        if (fqn == null || fqn.isBlank()) return "Object";

        // Array types
        if (fqn.endsWith("[]")) {
            return simplify(fqn.substring(0, fqn.length() - 2), imports) + "[]";
        }

        // Generic types: e.g. java.util.List<java.lang.String>
        int angle = fqn.indexOf('<');
        if (angle >= 0) {
            String base = fqn.substring(0, angle);
            String inner = fqn.substring(angle + 1, fqn.length() - 1);
            String simpleBase = simplify(base, imports);
            List<String> args = splitGenericArgs(inner);
            StringBuilder sb = new StringBuilder(simpleBase).append('<');
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(simplify(args.get(i).trim(), imports));
            }
            sb.append('>');
            return sb.toString();
        }

        // Wildcards
        if (fqn.startsWith("?")) return fqn;

        // Primitives
        if (!fqn.contains(".")) return fqn;

        // java.lang — no import needed
        if (fqn.startsWith("java.lang.") && !fqn.substring("java.lang.".length()).contains(".")) {
            return fqn.substring("java.lang.".length());
        }

        // Everything else — add import, return simple name
        imports.add(fqn);
        return fqn.substring(fqn.lastIndexOf('.') + 1);
    }

    private static List<String> splitGenericArgs(String args) {
        List<String> result = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ',' && depth == 0) {
                result.add(args.substring(start, i));
                start = i + 1;
            }
        }
        result.add(args.substring(start));
        return result;
    }

    public static String toSnakeCase(String camelCase) {
        return camelCase.replaceAll("([A-Z])", "_$1").toLowerCase().replaceFirst("^_", "");
    }
}
