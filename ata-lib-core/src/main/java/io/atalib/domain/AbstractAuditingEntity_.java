package io.atalib.domain;

import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

import java.time.LocalDateTime;

@StaticMetamodel(AbstractAuditingEntity.class)
public abstract class AbstractAuditingEntity_ {

    public static volatile SingularAttribute<AbstractAuditingEntity, Long> id;
    public static volatile SingularAttribute<AbstractAuditingEntity, LocalDateTime> createdAt;
    public static volatile SingularAttribute<AbstractAuditingEntity, LocalDateTime> updatedAt;
    public static volatile SingularAttribute<AbstractAuditingEntity, String> createdBy;
    public static volatile SingularAttribute<AbstractAuditingEntity, String> updatedBy;
    public static volatile SingularAttribute<AbstractAuditingEntity, String> deletedBy;
    public static volatile SingularAttribute<AbstractAuditingEntity, Boolean> deleted;
    public static volatile MappedSuperclassType<AbstractAuditingEntity> class_;

    public static final String ID = "id";
    public static final String CREATED_AT = "createdAt";
    public static final String UPDATED_AT = "updatedAt";
    public static final String CREATED_BY = "createdBy";
    public static final String UPDATED_BY = "updatedBy";
    public static final String DELETED_BY = "deletedBy";
    public static final String DELETED = "deleted";
}
