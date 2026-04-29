package io.atalib.processor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EntityModel {

    static final Set<String> AUDIT_FIELDS = Set.of(
            "id", "createdAt", "updatedAt", "createdBy", "updatedBy", "deleted", "deletedBy"
    );

    private final String className;
    private final String packageName;
    private final String tableName;
    private final String baseUrl;
    private final List<FieldModel> fields;
    private final Set<String> responseExclude;
    private final Set<String> requestExclude;

    public EntityModel(String className, String packageName, String tableName, String baseUrl,
                       List<FieldModel> fields, Set<String> responseExclude, Set<String> requestExclude) {
        this.className = className;
        this.packageName = packageName;
        this.tableName = tableName;
        this.baseUrl = baseUrl;
        this.fields = fields;
        this.responseExclude = responseExclude;
        this.requestExclude = requestExclude;
    }

    // -------------------------------------------------------------------------
    // Computed names
    // -------------------------------------------------------------------------

    public String responseDtoName()  { return className + "ResponseDto"; }
    public String requestDtoName()   { return className + "RequestDto"; }
    public String mapperName()       { return className + "Mapper"; }
    public String repositoryName()   { return className + "Repository"; }
    public String serviceImplName()  { return className + "ServiceImpl"; }
    public String controllerName()   { return className + "Controller"; }

    // -------------------------------------------------------------------------
    // Filtered field views
    // -------------------------------------------------------------------------

    /** Fields included in the ResponseDto (entity fields minus audit + responseExclude). */
    public List<FieldModel> getResponseFields() {
        return fields.stream()
                .filter(f -> !AUDIT_FIELDS.contains(f.getName()))
                .filter(f -> !responseExclude.contains(f.getName()))
                .collect(Collectors.toList());
    }

    /** Fields included in the RequestDto (entity fields minus audit + id + requestExclude). */
    public List<FieldModel> getRequestFields() {
        return fields.stream()
                .filter(f -> !AUDIT_FIELDS.contains(f.getName()))
                .filter(f -> !requestExclude.contains(f.getName()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getClassName()   { return className; }
    public String getPackageName() { return packageName; }
    public String getTableName()   { return tableName; }
    public String getBaseUrl()     { return baseUrl; }
    public List<FieldModel> getFields() { return fields; }
}
