package io.atalib.annotation;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.lang.annotation.*;

/**
 * Composed annotation: {@code @Entity} + {@code @EntityListeners(AuditingEntityListener.class)}.
 *
 * <p>Also triggers {@code ata-lib-processor} which generates at compile time:
 * ResponseDto, RequestDto, Mapper (MapStruct), Repository, ServiceImpl and Controller.
 *
 * <p>Usage:
 * <pre>
 * {@code @AtaEntity(responseExclude = {"password"}, baseUrl = "/api/v1/users")}
 * {@code @Table(name = "users")}
 * {@code @Entity}
 * {@code @Getter @Setter @NoArgsConstructor}
 * public class User extends AbstractUuidAuditingEntity { ... }
 * </pre>
 *
 * <p>SQL table name: controlled by {@code @jakarta.persistence.Table(name = "...")}.
 * If omitted, JPA defaults to the class name lowercased. Always add {@code @Table}
 * when the class name is a reserved SQL keyword (e.g. {@code user}, {@code order}, {@code group}).
 *
 * <p>Note: Lombok does not process meta-annotations — {@code @Getter}, {@code @Setter},
 * etc. must be placed directly on the entity class, not through {@code @AtaEntity}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Entity
@EntityListeners(AuditingEntityListener.class)
public @interface AtaEntity {

    /** Fields to exclude from the generated ResponseDto. Audit fields are always excluded. */
    String[] responseExclude() default {};

    /** Fields to exclude from the generated RequestDto. {@code id} and audit fields are always excluded. */
    String[] requestExclude() default {};

    /** Base URL of the generated REST controller. Default: /{classNameLowercase}. */
    String baseUrl() default "";

    // -------------------------------------------------------------------------
    // Security — mapped to @SecuredCrud on the generated controller.
    // Empty array means no restriction for that operation.
    // Role values are prefixed with ROLE_ automatically by CrudSecurityAspect.
    // -------------------------------------------------------------------------

    /** Roles allowed to call POST (create). */
    String[] securedCreate() default {};

    /** Roles allowed to call PUT (update). */
    String[] securedUpdate() default {};

    /** Roles allowed to call DELETE. */
    String[] securedDelete() default {};

    /** Roles allowed to call GET /{id}. */
    String[] securedRead() default {};

    /** Roles allowed to call GET (list / getAll). */
    String[] securedList() default {};
}
