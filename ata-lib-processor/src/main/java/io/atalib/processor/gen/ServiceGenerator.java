package io.atalib.processor.gen;

import io.atalib.processor.EntityModel;

public final class ServiceGenerator {

    private ServiceGenerator() {}

    public static String generate(EntityModel model) {
        String pkg    = model.getPackageName();
        String entity = model.getClassName();
        String resp   = model.responseDtoName();
        String req    = model.requestDtoName();
        String repo   = model.repositoryName();
        String mapper = model.mapperName();
        String impl   = model.serviceImplName();

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(";\n\n");
        String idType = model.getIdTypeName();

        sb.append("import io.atalib.annotation.AtaService;\n");
        sb.append("import io.atalib.service.AbstractGenericService;\n");
        sb.append("import org.springframework.data.domain.Page;\n");
        sb.append("import org.springframework.data.domain.PageRequest;\n");
        sb.append("import org.springframework.data.domain.Pageable;\n");
        sb.append("import org.springframework.data.domain.Sort;\n");
        sb.append("import java.util.List;\n");
        sb.append("import java.util.stream.Collectors;\n");
        for (String imp : model.getIdTypeImports()) {
            sb.append("import ").append(imp).append(";\n");
        }
        sb.append("\n");
        sb.append("@AtaService\n");
        sb.append("public class ").append(impl)
                .append(" extends AbstractGenericService<")
                .append(entity).append(", ").append(req).append(", ").append(resp).append(", ").append(idType).append("> {\n\n");

        sb.append("    private final ").append(repo).append(" repository;\n");
        sb.append("    private final ").append(mapper).append(" mapper;\n\n");

        sb.append("    public ").append(impl).append("(").append(repo).append(" repository, ").append(mapper).append(" mapper) {\n");
        sb.append("        super(repository, mapper::toEntity, mapper::toDto, mapper::updateEntity);\n");
        sb.append("        this.repository = repository;\n");
        sb.append("        this.mapper = mapper;\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    protected Page<").append(entity).append("> fetchEntities(Pageable pageable) {\n");
        sb.append("        Sort sort = Sort.by(Sort.Direction.DESC, \"updatedAt\");\n");
        sb.append("        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);\n");
        sb.append("        return repository.findAllByDeletedFalse(pageRequest);\n");
        sb.append("    }\n\n");

        sb.append("    @Override\n");
        sb.append("    public List<").append(resp).append("> getAllWithoutPagination() {\n");
        sb.append("        return repository.findAllByDeletedFalse()\n");
        sb.append("                .stream()\n");
        sb.append("                .map(mapper::toDto)\n");
        sb.append("                .collect(Collectors.toList());\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }
}
