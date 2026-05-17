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
    private final List<String> securedCreate;
    private final List<String> securedUpdate;
    private final List<String> securedDelete;
    private final List<String> securedRead;
    private final List<String> securedList;
    private final String apiTag;
    private final String apiDescription;

    public EntityModel(String className, String packageName, String baseUrl,
                       List<FieldModel> fields, Set<String> responseExclude, Set<String> requestExclude,
                       String idTypeName, List<String> idTypeImports,
                       List<String> securedCreate, List<String> securedUpdate, List<String> securedDelete,
                       List<String> securedRead, List<String> securedList,
                       String apiTag, String apiDescription) {
        this.className = className;
        this.packageName = packageName;
        this.baseUrl = baseUrl;
        this.fields = fields;
        this.responseExclude = responseExclude;
        this.requestExclude = requestExclude;
        this.idTypeName = idTypeName;
        this.idTypeImports = idTypeImports;
        this.securedCreate = securedCreate;
        this.securedUpdate = securedUpdate;
        this.securedDelete = securedDelete;
        this.securedRead = securedRead;
        this.securedList = securedList;
        this.apiTag = apiTag;
        this.apiDescription = apiDescription;
    }

    public boolean hasSecurity() {
        return !securedCreate.isEmpty() || !securedUpdate.isEmpty() || !securedDelete.isEmpty()
                || !securedRead.isEmpty() || !securedList.isEmpty();
    }

    public List<String> getSecuredCreate() { return securedCreate; }
    public List<String> getSecuredUpdate() { return securedUpdate; }
    public List<String> getSecuredDelete() { return securedDelete; }
    public List<String> getSecuredRead()   { return securedRead; }
    public List<String> getSecuredList()   { return securedList; }
    public String getApiTag()              { return apiTag; }
    public String getApiDescription()      { return apiDescription; }
    public boolean hasApiTag()             { return apiTag != null && !apiTag.isBlank(); }

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
