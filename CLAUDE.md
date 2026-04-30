# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
mvn clean install        # Build all modules and install to local repo
mvn clean package        # Package JARs only
mvn clean install -pl ata-lib-core          # Build core module only
mvn clean install -pl ata-lib-spring-boot-starter  # Build starter only
```

No tests currently exist. The library is validated by integration in consuming projects.

## Architecture Overview

**ata-lib** is a dual-module Maven library that eliminates CRUD boilerplate in Spring Boot 3.3+ / Java 17 microservices.

```
ata-lib-core               → Pure abstractions, no auto-configuration
ata-lib-spring-boot-starter → Wraps core + adds @AutoConfiguration
```

Consumers choose one: `starter` for new projects (zero-config), `core` for existing projects needing manual wiring.

### Layer Stack (bottom to top)

| Layer | Key Type | Package |
|-------|----------|---------|
| Domain | `AbstractAuditingBase`, `AbstractAuditingEntity`, `AbstractUuidAuditingEntity` | `io.atalib.domain` |
| Mapping | `EntityMapper<D,E>` | `io.atalib.mapper` |
| Service | `AbstractGenericService<E,REQ,RES,ID>` | `io.atalib.service` |
| Controller | `AbstractGenericController<REQ,RES,ID>` | `io.atalib.controller` |
| Security | `@SecuredCrud` + `CrudSecurityAspect` | `io.atalib.security` |
| Auto-config | `AtaLibAutoConfiguration` | `io.atalib.autoconfigure` |

### Core Design Patterns

**Template Method** — `AbstractGenericService` defines the CRUD algorithm. Subclasses override lifecycle hooks: `beforeCreate`, `afterMapping`, `afterCreate`, `beforeUpdate`, `afterUpdateMapping`, `afterUpdate`, `beforeDelete`, `afterDelete`.

**Strategy via constructor injection** — `AbstractGenericService` takes mapper functions as constructor parameters (`Function<REQ, E>`, `Function<E, RES>`, `BiConsumer<E, REQ>`), decoupling the service from specific MapStruct mappers.

**AOP Security** — `CrudSecurityAspect` intercepts controller methods via `@Before` pointcuts, checking `@SecuredCrud` attributes (roles and permissions are OR'd). No authorization logic leaks into business code.

**Soft Delete** — `AbstractAuditingBase` has a `deleted` (Boolean, default false) field and a `softDelete()` method. Services call this instead of physical deletion. Consuming projects must filter `deleted=false` in queries (e.g., `findAllByDeletedFalse()`).

**Domain hierarchy** — `AbstractAuditingBase` holds all audit fields and lifecycle hooks (`@PrePersist`, `@PreUpdate`, `@PreRemove`, `softDelete()`). Two concrete base classes extend it: `AbstractAuditingEntity` (adds `@Id Long id` with `IDENTITY` generation) and `AbstractUuidAuditingEntity` (adds `@Id UUID id` with `UUID` generation). Entities extend whichever matches their ID strategy. `AbstractGenericService.delete()` checks `instanceof AbstractAuditingBase` to support both.

**Auditing** — `@CreatedDate`, `@LastModifiedDate`, `createdBy`, `updatedBy`, `deletedBy` are populated automatically via `AuditingEntityListener` + `SecurityContextHolder` (falls back to `"SYSTEM"`).

### Auto-configuration (starter only)

`AtaLibAutoConfiguration` activates:
- `@EnableJpaAuditing` — wires audit fields
- `@EnableAspectJAutoProxy` — enables AOP
- `CrudSecurityAspect` bean (conditionally, when Spring Security is present)

### Composed Meta-Annotations

| Annotation | Expands to |
|-----------|-----------|
| `@AtaEntity` | `@Entity + @EntityListeners(AuditingEntityListener.class)` |
| `@AtaService` | `@Service + @Transactional` |
| `@AtaController(path)` | `@RestController + @RequestMapping(path)` |
| `@AtaMapper` | `@Mapper(componentModel = "spring")` |

## Critical Annotation Processor Constraints

**Lombok must be declared before MapStruct** in the Maven compiler plugin annotation processor list. This ordering is enforced in the parent `pom.xml` and must be preserved.

## Known Limitations

These are documented limitations — do not attempt to "fix" them by changing library behavior:

1. **`@AtaEntity` is not enough for entity scanning** — Spring Boot's entity scanner uses `considerMetaAnnotations=false`. Entities must carry `@Entity` directly in addition to (or instead of) `@AtaEntity`.

2. **`@AtaMapper` doesn't trigger MapStruct code generation** — MapStruct's APT only detects direct `@Mapper`. Mappers must carry `@Mapper(componentModel = "spring")` directly.

3. **Lombok meta-annotations not processed** — `@Getter`, `@Setter`, `@Builder`, etc. must appear directly on entity classes, not via composed annotations.

4. **`@Builder` conflicts with inherited fields** — Use `@SuperBuilder` on entities extending `AbstractAuditingEntity` to include inherited fields in MapStruct mappings.

## Deployment

The library is distributed via JitPack. Publishing is triggered by pushing a git tag. The `jitpack.yml` pins OpenJDK 17.
