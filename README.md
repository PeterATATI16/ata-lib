# ata-lib

Generic CRUD library for Spring Boot projects.

Provides reusable abstractions for entity, service, controller and security layers — eliminating CRUD boilerplate across microservices. With the annotation processor, a single annotated entity generates a complete REST API automatically.

---

## What you get

Write this :

```java
@AtaEntity(table = "staff", responseExclude = {"password"}, baseUrl = "/api/v1/staff")
@Getter @Setter @NoArgsConstructor
@Entity
public class Staff extends AbstractAuditingEntity {
    @NotBlank private String firstName;
    @Email    private String email;
    private   String password;
}
```

Get this automatically :

| Generated file | Content |
|---|---|
| `StaffResponseDto` | All fields + `id`, excluding `password` |
| `StaffRequestDto` | All fields, excluding `id` and audit fields |
| `StaffMapper` | MapStruct interface — `toDto`, `toEntity`, `updateEntity` |
| `StaffRepository` | `JpaRepository<Staff, Long>` + `findAllByDeletedFalse()` |
| `StaffServiceImpl` | Extends `AbstractGenericService`, full CRUD logic |
| `StaffController` | Extends `AbstractGenericController`, 5 REST endpoints |

**5 endpoints ready, 0 boilerplate written.**

---

## Installation

### 1. Add the JitPack repository

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### 2. Choose your dependency

**Option A — New project** (no existing JPA/AOP config) :

```xml
<dependency>
    <groupId>com.github.PeterATATI16.ata-lib</groupId>
    <artifactId>ata-lib-spring-boot-starter</artifactId>
    <version>v1.1.0</version>
</dependency>
```

**Option B — Existing project** (JHipster, custom Spring config, etc.) :

```xml
<dependency>
    <groupId>com.github.PeterATATI16.ata-lib</groupId>
    <artifactId>ata-lib-core</artifactId>
    <version>v1.1.0</version>
</dependency>
```

> See [Starter vs Core](#starter-vs-core) below for guidance.

### 3. Configure annotation processors

In `maven-compiler-plugin > annotationProcessorPaths` — **order matters** : Lombok → MapStruct → ata-lib-processor.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
            <path>
                <groupId>com.github.PeterATATI16.ata-lib</groupId>
                <artifactId>ata-lib-processor</artifactId>
                <version>v1.1.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

Also add Lombok as a compile dependency if not already present :

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

---

## Quick Start

### 1. Write your entity

```java
@AtaEntity(
    table           = "staff",
    responseExclude = {"password"},                 // excluded from ResponseDto
    requestExclude  = {"id"},                       // excluded from RequestDto (audit fields always excluded)
    baseUrl         = "/api/v1/staff"               // controller base URL
)
@Getter @Setter @NoArgsConstructor
@Entity                                             // required directly — Spring's entity scanner does not follow meta-annotations
public class Staff extends AbstractAuditingEntity {

    @Column(length = 100)
    @NotBlank @Size(max = 100)
    private String firstName;

    @Column(length = 100)
    @NotBlank @Size(max = 100)
    private String lastName;

    @Column(unique = true, length = 150)
    @Email @NotBlank
    private String email;

    private String password;
}
```

`AbstractAuditingEntity` provides automatically : `id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deletedBy`, `deleted`.

### 2. That's it

Build the project — the processor generates 6 files. The 5 REST endpoints are immediately available :

| Method | Path | Action |
|--------|------|--------|
| `POST`   | `/api/v1/staff`      | Create |
| `PUT`    | `/api/v1/staff/{id}` | Update |
| `GET`    | `/api/v1/staff/{id}` | Get by ID |
| `GET`    | `/api/v1/staff`      | Get all (paginated) |
| `DELETE` | `/api/v1/staff/{id}` | Soft delete |

### 3. Customize what you need

Override only what differs from the default behaviour.

**Custom service logic** — extend the generated `ServiceImpl` in a different package and annotate with `@Primary` :

```java
// Your package: com.example.service (not the entity's package)
@Service
@Primary
public class StaffService extends StaffServiceImpl {

    public StaffService(StaffRepository repository, StaffMapper mapper) {
        super(repository, mapper);
    }

    @Override
    protected void beforeCreate(StaffRequestDto dto) {
        // hash password, check uniqueness, etc.
    }
}
```

**Security on generated endpoints** — configure Spring Security globally (recommended for generated controllers) :

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST,   "/api/v1/staff").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT,    "/api/v1/staff/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/v1/staff/**").hasRole("ADMIN")
            .anyRequest().authenticated());
        return http.build();
    }
}
```

**Security with `@SecuredCrud` / additional endpoints** — write a full manual controller. In this case, exclude the generated controller from your component scan and write your own extending `AbstractGenericController` directly (see [Manual mode](#manual-mode-without-processor)) :

```java
@AtaController("/api/v1/staff")
@SecuredCrud(create = {"ADMIN"}, update = {"ADMIN"}, delete = {"ADMIN"})
public class StaffController
        extends AbstractGenericController<StaffRequestDto, StaffResponseDto, Long> {

    public StaffController(StaffService service) { super(service); }

    @GetMapping("/search")
    public ResponseEntity<List<StaffResponseDto>> search(@RequestParam String q) {
        // custom endpoint
    }
}
```

---

## @AtaEntity attributes

| Attribute | Type | Default | Description |
|---|---|---|---|
| `table` | `String` | snake_case of class name | SQL table name |
| `responseExclude` | `String[]` | `{}` | Fields to exclude from `ResponseDto`. Audit fields are always excluded. |
| `requestExclude` | `String[]` | `{}` | Fields to exclude from `RequestDto`. `id` + audit fields are always excluded. |
| `baseUrl` | `String` | `/{classNameLowercase}` | Controller base URL |

**Fields always excluded from ResponseDto** : `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deleted`, `deletedBy`

**Fields always excluded from RequestDto** : same + `id`

---

## Lifecycle hooks

Override any hook in a custom `ServiceImpl` :

```java
protected void beforeCreate(REQUEST_DTO dto)                      {}
protected void afterMapping(ENTITY entity, REQUEST_DTO dto)       {}
protected void afterCreate(ENTITY entity, REQUEST_DTO dto)        {}
protected void beforeUpdate(ID id, REQUEST_DTO dto)               {}
protected void afterUpdateMapping(ENTITY entity, REQUEST_DTO dto) {}
protected void afterUpdate(ENTITY entity, REQUEST_DTO dto)        {}
protected void beforeDelete(ID id)                                {}
protected void afterDelete(ID id)                                 {}
```

---

## Starter vs Core

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

## Available annotations

| Annotation | Equivalent |
|---|---|
| `@AtaEntity(...)` | `@Entity + @EntityListeners(AuditingEntityListener.class)` + triggers processor |
| `@AtaService` | `@Service + @Transactional` |
| `@AtaController(path)` | `@RestController + @RequestMapping(path)` |
| `@SecuredCrud` | Declarative role/permission security on CRUD endpoints |

---

## Manual mode (without processor)

If you prefer full control and want to write each layer yourself, omit `ata-lib-processor` from `annotationProcessorPaths`. Refer to `USAGE-EXAMPLE.java` in the repository for the complete manual example.

---

## Module structure

```
ata-lib-parent
├── ata-lib-core               ← abstractions (no auto-configuration)
│   ├── io.atalib.domain       → AbstractAuditingEntity
│   ├── io.atalib.service      → GenericService, AbstractGenericService
│   ├── io.atalib.controller   → AbstractGenericController
│   ├── io.atalib.security     → @SecuredCrud, CrudSecurityAspect
│   ├── io.atalib.annotation   → @AtaEntity, @AtaService, @AtaController
│   ├── io.atalib.exception    → EntityNotFoundException
│   └── io.atalib.util         → AuditUtils
├── ata-lib-processor          ← annotation processor (compile-time code generation)
│   └── io.atalib.processor    → AtaEntityProcessor + generators
└── ata-lib-spring-boot-starter  ← auto-configuration (@EnableJpaAuditing, AOP, security aspect)
```

---

## Known limitations

- **`@Entity` must be direct** : Spring Boot's entity scanner does not follow meta-annotations. Place `@Entity` directly on the class in addition to `@AtaEntity`.
- **Lombok annotations must be direct** : `@Getter`, `@Setter`, etc. must be placed directly on the entity class.
- **`@Builder` conflicts with inherited fields** : do not use `@Builder` on entities. Use `@SuperBuilder` if a builder is required.

---

## Stack

- Java 17
- Spring Boot 3.3+
- Spring Data JPA
- Spring Security (optional — required for `@SecuredCrud`)
- MapStruct 1.5+
- Lombok 1.18+
