package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;
import io.atalib.processor.FieldModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoGeneratorTest {

    private static EntityModel model(Set<String> responseExclude, Set<String> requestExclude) {
        List<FieldModel> fields = List.of(
                new FieldModel("firstName", "String", List.of(), List.of()),
                new FieldModel("email", "String", List.of(), List.of()),
                new FieldModel("password", "String", List.of(), List.of())
        );
        return new EntityModel(
                "Staff", "com.example.domain", "/api/v1/staff",
                fields, responseExclude, requestExclude,
                "Long", List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }

    @Test
    void responseDto_includesIdField() {
        String code = DtoGenerator.generateResponse(model(Set.of(), Set.of()));
        assertThat(code).contains("private Long id;");
    }

    @Test
    void requestDto_doesNotIncludeIdField() {
        String code = DtoGenerator.generateRequest(model(Set.of(), Set.of()));
        assertThat(code).doesNotContain("private Long id;");
    }

    @Test
    void responseDto_excludesConfiguredFields() {
        String code = DtoGenerator.generateResponse(model(Set.of("password"), Set.of()));
        assertThat(code)
                .contains("firstName")
                .contains("email")
                .doesNotContain("password");
    }

    @Test
    void requestDto_excludesConfiguredFields() {
        String code = DtoGenerator.generateRequest(model(Set.of(), Set.of("email")));
        assertThat(code)
                .contains("firstName")
                .contains("password")
                .doesNotContain("private String email;");
    }

    @Test
    void responseDto_generatesGettersAndSetters() {
        String code = DtoGenerator.generateResponse(model(Set.of(), Set.of()));
        assertThat(code)
                .contains("getFirstName()")
                .contains("setFirstName(")
                .contains("getId()")
                .contains("setId(");
    }

    @Test
    void responseDto_generatesNoArgsAndAllArgsConstructors() {
        String code = DtoGenerator.generateResponse(model(Set.of(), Set.of()));
        assertThat(code).contains("public StaffResponseDto() {}");
        assertThat(code).contains("public StaffResponseDto(Long id,");
    }

    @Test
    void responseDto_auditFieldsAreAlwaysExcluded() {
        String code = DtoGenerator.generateResponse(model(Set.of(), Set.of()));
        assertThat(code)
                .doesNotContain("createdAt")
                .doesNotContain("updatedAt")
                .doesNotContain("createdBy")
                .doesNotContain("deletedBy");
    }
}
