package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;

public final class RepositoryGenerator {

    private RepositoryGenerator() {}

    public static String generate(EntityModel model) {
        String pkg    = model.getPackageName();
        String entity = model.getClassName();
        String repo   = model.repositoryName();

        String idType = model.getIdTypeName();
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.springframework.data.domain.Page;\n");
        sb.append("import org.springframework.data.domain.Pageable;\n");
        sb.append("import org.springframework.data.jpa.repository.JpaRepository;\n");
        sb.append("import org.springframework.stereotype.Repository;\n");
        sb.append("import java.util.List;\n");
        for (String imp : model.getIdTypeImports()) {
            sb.append("import ").append(imp).append(";\n");
        }
        sb.append("\n@Repository\n");
        sb.append("public interface ").append(repo).append(" extends JpaRepository<").append(entity).append(", ").append(idType).append("> {\n\n");
        sb.append("    Page<").append(entity).append("> findAllByDeletedFalse(Pageable pageable);\n\n");
        sb.append("    List<").append(entity).append("> findAllByDeletedFalse();\n");
        sb.append("}\n");
        return sb.toString();
    }
}
