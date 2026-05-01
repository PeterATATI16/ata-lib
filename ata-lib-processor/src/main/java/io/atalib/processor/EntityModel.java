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
    private final String baseUrl;
    private final List<FieldModel> fields;
    private final Set<String> responseExclude;
    private final Set<String> requestExclude;
    private final String idTypeName;
    private final List<String> idTypeImports;

    public EntityModel(String className, String packageName, String baseUrl,
                       List<FieldModel> fields, Set<String> responseExclude, Set<String> requestExclude,
                       String idTypeName, List<String> idTypeImports) {
        this.className = className;
        this.packageName = packageName;
        this.baseUrl = baseUrl;
        this.fields = fields;
        this.responseExclude = responseExclude;
        this.requestExclude = requestExclude;
        this.idTypeName = idTypeName;
        this.idTypeImports = idTypeImports;
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

    public List<FieldModel> getResponseFields() {
        return fields.stream()
                .filter(f -> !AUDIT_FIELDS.contains(f.getName()))
                .filter(f -> !responseExclude.contains(f.getName()))
                .collect(Collectors.toList());
    }

    public List<FieldModel> getRequestFields() {
        return fields.stream()
                .filter(f -> !AUDIT_FIELDS.contains(f.getName()))
                .filter(f -> !requestExclude.contains(f.getName()))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getClassName()           { return className; }
    public String getPackageName()         { return packageName; }
    /** Package where all generated files are written — one subpackage per entity. */
    public String getGeneratedPackageName() { return packageName + "." + className.toLowerCase(); }
    /** Fully-qualified entity class name, used as import in generated files. */
    public String getEntityFqn()           { return packageName + "." + className; }
    public String getBaseUrl()             { return baseUrl; }
    public List<FieldModel> getFields()    { return fields; }
    public String getIdTypeName()          { return idTypeName; }
    public List<String> getIdTypeImports() { return idTypeImports; }
}
