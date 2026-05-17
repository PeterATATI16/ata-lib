package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;
import io.atalib.processor.FieldModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ControllerGeneratorTest {

    private static EntityModel model(List<String> securedCreate, List<String> securedUpdate,
                                     List<String> securedDelete, List<String> securedRead,
                                     List<String> securedList) {
        return new EntityModel(
                "Staff", "com.example.domain", "/api/v1/staff",
                List.of(new FieldModel("name", "String", List.of(), List.of())),
                Set.of(), Set.of(),
                "Long", List.of(),
                securedCreate, securedUpdate, securedDelete, securedRead, securedList
        );
    }

    @Test
    void noSecurity_doesNotEmitSecuredCrud() {
        String code = ControllerGenerator.generate(
                model(List.of(), List.of(), List.of(), List.of(), List.of()));
        assertThat(code)
                .doesNotContain("@SecuredCrud")
                .doesNotContain("import io.atalib.security.SecuredCrud");
    }

    @Test
    void withSecurity_emitsSecuredCrudAnnotation() {
        String code = ControllerGenerator.generate(
                model(List.of("ADMIN"), List.of(), List.of("ADMIN"), List.of(), List.of()));
        assertThat(code)
                .contains("@SecuredCrud(")
                .contains("import io.atalib.security.SecuredCrud");
    }

    @Test
    void withSecurity_emitsOnlyConfiguredOperations() {
        String code = ControllerGenerator.generate(
                model(List.of("ADMIN"), List.of("ADMIN", "MANAGER"), List.of("ADMIN"),
                      List.of(), List.of()));
        assertThat(code)
                .contains("create = {\"ADMIN\"}")
                .contains("update = {\"ADMIN\", \"MANAGER\"}")
                .contains("delete = {\"ADMIN\"}")
                .doesNotContain("read =")
                .doesNotContain("list =");
    }

    @Test
    void controller_extendsAbstractGenericController() {
        String code = ControllerGenerator.generate(
                model(List.of(), List.of(), List.of(), List.of(), List.of()));
        assertThat(code).contains("extends AbstractGenericController<StaffRequestDto, StaffResponseDto, Long>");
    }

    @Test
    void controller_usesAtaControllerWithBaseUrl() {
        String code = ControllerGenerator.generate(
                model(List.of(), List.of(), List.of(), List.of(), List.of()));
        assertThat(code).contains("@AtaController(\"/api/v1/staff\")");
    }
}
