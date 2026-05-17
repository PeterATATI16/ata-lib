package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;
import io.atalib.processor.FieldModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceGeneratorTest {

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
    void service_extendsAbstractGenericService() {
        String code = ServiceGenerator.generate(model());
        assertThat(code).contains(
                "extends AbstractGenericService<Staff, StaffRequestDto, StaffResponseDto, Long>");
    }

    @Test
    void service_delegatesToMapperMethods() {
        String code = ServiceGenerator.generate(model());
        assertThat(code)
                .contains("mapper::toEntity")
                .contains("mapper::toDto")
                .contains("mapper::updateEntity");
    }

    @Test
    void fetchEntities_usesDeletedFalseFilter() {
        String code = ServiceGenerator.generate(model());
        assertThat(code).contains("findAllByDeletedFalse(pageRequest)");
    }

    @Test
    void getAllWithoutPagination_usesDeletedFalseFilter() {
        String code = ServiceGenerator.generate(model());
        assertThat(code).contains("findAllByDeletedFalse()");
    }

    @Test
    void service_isAnnotatedWithAtaService() {
        String code = ServiceGenerator.generate(model());
        assertThat(code).contains("@AtaService");
    }
}
