package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;

public final class MapperGenerator {

    private static final String[] AUDIT_IGNORES = {
            "createdAt", "updatedAt", "createdBy", "updatedBy", "deleted", "deletedBy"
    };

    private MapperGenerator() {}

    public static String generate(EntityModel model) {
        String pkg   = model.getPackageName();
        String entity = model.getClassName();
        String resp  = model.responseDtoName();
        String req   = model.requestDtoName();
        String mapper = model.mapperName();

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        sb.append("import org.mapstruct.BeanMapping;\n");
        sb.append("import org.mapstruct.Mapper;\n");
        sb.append("import org.mapstruct.Mapping;\n");
        sb.append("import org.mapstruct.MappingTarget;\n");
        sb.append("import org.mapstruct.NullValuePropertyMappingStrategy;\n");
        sb.append("\n");
        sb.append("@Mapper(componentModel = \"spring\")\n");
        sb.append("public interface ").append(mapper).append(" {\n\n");

        // toDto — straightforward, id is mapped automatically (same name)
        sb.append("    ").append(resp).append(" toDto(").append(entity).append(" entity);\n\n");

        // toEntity — ignore audit fields
        appendAuditIgnoreMappings(sb, "    ");
        sb.append("    ").append(entity).append(" toEntity(").append(req).append(" dto);\n\n");

        // updateEntity — partial update, ignore audit fields
        sb.append("    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)\n");
        appendAuditIgnoreMappings(sb, "    ");
        sb.append("    void updateEntity(@MappingTarget ").append(entity).append(" entity, ").append(req).append(" dto);\n");

        sb.append("}\n");
        return sb.toString();
    }

    private static void appendAuditIgnoreMappings(StringBuilder sb, String indent) {
        for (String field : AUDIT_IGNORES) {
            sb.append(indent).append("@Mapping(target = \"").append(field).append("\", ignore = true)\n");
        }
    }
}
