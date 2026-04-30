package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;

public final class RepositoryGenerator {

    private RepositoryGenerator() {}

    public static String generate(EntityModel model) {
        String pkg    = model.getPackageName();
        String entity = model.getClassName();
        String repo   = model.repositoryName();

        return "package " + pkg + ";\n\n"
                + "import org.springframework.data.domain.Page;\n"
                + "import org.springframework.data.domain.Pageable;\n"
                + "import org.springframework.data.jpa.repository.JpaRepository;\n"
                + "import org.springframework.stereotype.Repository;\n"
                + "import java.util.List;\n\n"
                + "@Repository\n"
                + "public interface " + repo + " extends JpaRepository<" + entity + ", Long> {\n\n"
                + "    Page<" + entity + "> findAllByDeletedFalse(Pageable pageable);\n\n"
                + "    List<" + entity + "> findAllByDeletedFalse();\n"
                + "}\n";
    }
}
