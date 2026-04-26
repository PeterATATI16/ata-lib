# ata-lib

Generic CRUD library for Spring Boot projects.

Provides reusable abstractions for entity, service, controller and security layers — eliminating CRUD boilerplate across microservices.

---

## Installation

### Via JitPack

Add the repository in your `pom.xml` :

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then add the dependency — **choose one** based on your project type (see below) :

```xml
<!-- Option A — New project (no existing JPA/AOP config) -->
<dependency>
    <groupId>com.github.PeterATATI16</groupId>
    <artifactId>ata-lib-spring-boot-starter</artifactId>
    <version>v1.0.1</version>
</dependency>

<!-- Option B — Existing project (JHipster, custom Spring config, etc.) -->
<dependency>
    <groupId>com.github.PeterATATI16</groupId>
    <artifactId>ata-lib-core</artifactId>
    <version>v1.0.1</version>
</dependency>
```

Also add Lombok (if not already present) :

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

And in `maven-compiler-plugin > annotationProcessorPaths` — Lombok **must come before** MapStruct :

```xml
<path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></path>
<path><groupId>org.mapstruct</groupId><artifactId>mapstruct-processor</artifactId><version>...</version></path>
```

---

## Starter vs Core — which one to use?

| Condition | Use |
|---|---|
| New Spring Boot project, no existing JPA/AOP setup | `ata-lib-spring-boot-starter` |
| JHipster project | `ata-lib-core` |
| Project with `@EnableJpaAuditing` already declared | `ata-lib-core` |
| Project with `@EnableAspectJAutoProxy` already declared | `ata-lib-core` |
| Full control over bean registration | `ata-lib-core` |

**Rule of thumb** : if your project already has a `DatabaseConfiguration` or any class annotated with `@EnableJpaAuditing`, use `ata-lib-core` to avoid duplicate bean conflicts.

### When using `ata-lib-core` — 3 additional steps

**1.** Extend entity scanning to your domain package :

```java
@EntityScan(basePackages = { "com.existing.entity", "com.yourapp.domain" })
```

**2.** Extend repository scanning :

```java
@EnableJpaRepositories({ "com.existing.repository", "com.yourapp.repository" })
```

**3.** Register `CrudSecurityAspect` manually (only if you use `@SecuredCrud`) :

```java
@Configuration
public class AtaLibConfiguration {
    @Bean
    public CrudSecurityAspect crudSecurityAspect() {
        return new CrudSecurityAspect();
    }
}
```

---

## Quick Start — full example (Staff entity)

### Entity

```java
@Entity
@Table(name = "staff")
@Getter @Setter @NoArgsConstructor
public class Staff extends AbstractAuditingEntity {
    @Column(name = "first_name", length = 100) private String firstName;
    @Column(name = "last_name",  length = 100) private String lastName;
    @Column(name = "email",      length = 150, unique = true) private String email;
}
```

> `AbstractAuditingEntity` provides automatically : `id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deletedBy`, `deleted`.

### DTOs

```java
public class StaffRequestDto {
    @NotBlank @Size(max = 100) private String firstName;
    @NotBlank @Size(max = 100) private String lastName;
    @Email    @Size(max = 150) private String email;
    // getters / setters
}

public class StaffResponseDto {
    private Long id;
    private String firstName, lastName, email;
    private LocalDateTime createdAt, updatedAt;
    private String createdBy;
    // getters / setters
}
```

### Mapper

```java
@Mapper(componentModel = "spring")
public interface StaffMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted",   ignore = true)
    Staff toEntity(StaffRequestDto dto);

    StaffResponseDto toResponseDto(Staff staff);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted",   ignore = true)
    void updateEntity(@MappingTarget Staff entity, StaffRequestDto dto);
}
```

### Repository

```java
@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Page<Staff> findAllByDeletedFalse(Pageable pageable);
}
```

### Service

```java
public interface StaffService extends GenericService<StaffRequestDto, StaffResponseDto, Long> {}

@AtaService
public class StaffServiceImpl
        extends AbstractGenericService<Staff, StaffRequestDto, StaffResponseDto, Long>
        implements StaffService {

    public StaffServiceImpl(StaffRepository repo, StaffMapper mapper) {
        super(repo, mapper::toEntity, mapper::toResponseDto, mapper::updateEntity);
    }

    @Override
    protected Page<Staff> fetchEntities(Pageable pageable) {
        return ((StaffRepository) repository).findAllByDeletedFalse(pageable);
    }

    @Override
    public List<StaffResponseDto> getAllWithoutPagination() {
        return repository.findAll().stream()
                .filter(s -> !Boolean.TRUE.equals(s.getDeleted()))
                .map(entityToDtoMapper)
                .toList();
    }
}
```

### Controller

```java
@AtaController("/api/v1/staff")
@Tag(name = "Staff")
@SecuredCrud(
    create = {"ADMIN"},
    update = {"ADMIN"},
    delete = {"ADMIN"}
)
public class StaffController
        extends AbstractGenericController<StaffRequestDto, StaffResponseDto, Long> {

    public StaffController(StaffService service) {
        super(service);
    }
}
```

**Result** — 5 REST endpoints available automatically :

| Method | Path | Action | Access |
|--------|------|--------|--------|
| `POST` | `/api/v1/staff` | Create | ADMIN |
| `PUT` | `/api/v1/staff/{id}` | Update | ADMIN |
| `GET` | `/api/v1/staff/{id}` | Get by ID | Authenticated |
| `GET` | `/api/v1/staff?page=0` | Get all (paginated) | Authenticated |
| `DELETE` | `/api/v1/staff/{id}` | Soft delete | ADMIN |

---

## Available annotations

| Annotation | Equivalent | Works via meta-annotation? |
|---|---|---|
| `@AtaService` | `@Service + @Transactional` | ✅ Yes |
| `@AtaController(path)` | `@RestController + @RequestMapping(path)` | ✅ Yes |
| `@SecuredCrud` | Declarative role/permission security on CRUD | ✅ Yes (AOP) |
| `@AtaEntity` | `@Entity + @EntityListeners(AuditingEntityListener.class)` | ⚠️ Spring scans `@Entity` directly — keep `@Entity` on the class |

---

## Lifecycle hooks (AbstractGenericService)

Override any hook in your `ServiceImpl` :

```java
protected void beforeCreate(REQUEST_DTO dto)                     {}
protected void afterMapping(ENTITY entity, REQUEST_DTO dto)      {}
protected void afterCreate(ENTITY entity, REQUEST_DTO dto)       {}
protected void beforeUpdate(ID id, REQUEST_DTO dto)              {}
protected void afterUpdateMapping(ENTITY entity, REQUEST_DTO dto){}
protected void afterUpdate(ENTITY entity, REQUEST_DTO dto)       {}
protected void beforeDelete(ID id)                               {}
protected void afterDelete(ID id)                                {}
```

---

## Known limitations

- **Lombok meta-annotations** : Lombok does not process annotations declared on composed annotations. `@Getter`, `@Setter`, etc. must be placed directly on the entity class.
- **`@AtaMapper`** : MapStruct's annotation processor does not detect `@Mapper` through meta-annotations. Use `@Mapper(componentModel = "spring")` directly.
- **`@Entity` detection** : Spring Boot's entity scan uses `AnnotationTypeFilter` with `considerMetaAnnotations=false`. Place `@Entity` directly on the class, alongside `@AtaEntity` if desired.

---

## Module structure

```
ata-lib-parent
├── ata-lib-core               ← all abstractions (no auto-configuration)
│   ├── io.atalib.domain       → AbstractAuditingEntity
│   ├── io.atalib.service      → GenericService, AbstractGenericService
│   ├── io.atalib.controller   → AbstractGenericController
│   ├── io.atalib.security     → SecuredCrud, CrudSecurityAspect
│   ├── io.atalib.annotation   → @AtaEntity, @AtaService, @AtaController, @AtaMapper
│   ├── io.atalib.exception    → EntityNotFoundException
│   └── io.atalib.util         → AuditUtils
└── ata-lib-spring-boot-starter  ← auto-configuration (EnableJpaAuditing, EnableAspectJAutoProxy)
```

---

## Stack

- Java 17
- Spring Boot 3.3+
- Spring Data JPA
- Spring Security (optional — required for `@SecuredCrud`)
- MapStruct 1.5+
- Lombok 1.18+
