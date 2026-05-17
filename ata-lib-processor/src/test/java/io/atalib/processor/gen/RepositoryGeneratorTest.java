package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;
import io.atalib.processor.FieldModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryGeneratorTest {

    private static EntityModel model() {
        return new EntityModel(
                "Staff", "com.example.domain", "/api/v1/staff",
                List.of(new FieldModel("name", "String", List.of(), List.of())),
                Set.of(), Set.of(),
                "Long", List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                "", ""
        );
    }

    @Test
    void repository_extendsJpaRepositoryAndJpaSpecificationExecutor() {
        String code = RepositoryGenerator.generate(model());
        assertThat(code)
                .contains("extends JpaRepository<Staff, Long>")
                .contains("JpaSpecificationExecutor<Staff>");
    }

    @Test
    void repository_declaresDeletedFalseQueries() {
        String code = RepositoryGenerator.generate(model());
        assertThat(code)
                .contains("findAllByDeletedFalse(Pageable pageable)")
                .contains("findAllByDeletedFalse()");
    }

    @Test
    void repository_isAnnotatedWithRepository() {
        String code = RepositoryGenerator.generate(model());
        assertThat(code).contains("@Repository");
    }
}
