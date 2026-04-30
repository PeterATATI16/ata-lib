# ata-lib

**Eliminate CRUD boilerplate in Spring Boot microservices.**

Annotate an entity with `@AtaEntity` — the library generates the complete REST layer at compile time: DTOs, MapStruct mapper, repository, service, and controller. Zero runtime overhead. No reflection.

---

## What you get

Write this:

```java
@AtaEntity(responseExclude = {"password"}, baseUrl = "/api/v1/staff")
@Entity
@Getter @Setter @NoArgsConstructor
public class Staff extends AbstractAuditingEntity {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email @NotBlank
    private String email;

    private String password;
}
```

Get this, automatically, on every build:

| Generated file | What it contains |
|---|---|
| `StaffResponseDto` | `id` + all fields, excluding `password` and audit fields |
| `StaffRequestDto` | All fields, excluding `id`, `password`, and audit fields |
| `StaffMapper` | MapStruct interface — `toDto`, `toEntity`, `updateEntity` |
| `StaffRepository` | `JpaRepository<Staff, Long>` + `findAllByDeletedFalse()` |
| `StaffServiceImpl` | Full CRUD logic with soft delete and lifecycle hooks |
| `StaffController` | 5 REST endpoints, ready to use |

**5 REST endpoints. 0 boilerplate written.**

---

## Requirements

- Java 17+
- Spring Boot 3.3+
- Spring Data JPA
- MapStruct 1.5+
- Lombok 1.18+

---

## Step 1 — Add JitPack repository

In your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

---

## Step 2 — Add the dependency

**New project** (no existing JPA/AOP configuration):

```xml
<dependency>
    <groupId>com.github.PeterATATI16.ata-lib</groupId>
    <artifactId>ata-lib-spring-boot-starter</artifactId>
    <version>v1.1.0</version>
</dependency>
```

**Existing project** (JHipster, custom `@EnableJpaAuditing`, etc.):

```xml
<dependency>
    <groupId>com.github.PeterATATI16.ata-lib</groupId>
    <artifactId>ata-lib-core</artifactId>
    <version>v1.1.0</version>
</dependency>
```

> See [Starter vs Core](#starter-vs-core) for the full decision guide.

---

## Step 3 — Configure annotation processors

The processor order **matters**: Lombok must run before MapStruct, which must run before ata-lib-processor.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <!-- 1. Lombok first — generates getters/setters on your entity -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <!-- 2. MapStruct — generates mapper implementations -->
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${mapstruct.version}</version>
            </path>
            <!-- 3. ata-lib-processor — generates DTOs, Repository, Service, Controller -->
            <path>
                <groupId>com.github.PeterATATI16.ata-lib</groupId>
                <artifactId>ata-lib-processor</artifactId>
                <version>v1.1.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

Also declare Lombok as a compile dependency if not already present:

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

---

## Step 4 — Enable JPA auditing

**If you use the starter**, this is automatic — skip this step.

**If you use core**, add to your main configuration class:

```java
@Configuration
@EnableJpaAuditing
@EnableAspectJAutoProxy
public class AppConfiguration {
}
```

Also extend entity and repository scanning to include ata-lib's generated packages:

```java
@SpringBootApplication
@EntityScan(basePackages = {"com.yourapp.domain"})
@EnableJpaRepositories(basePackages = {"com.yourapp.domain"})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

---

## Step 5 — Write your entity

```java
@AtaEntity(
    responseExclude = {"password"},        // excluded from ResponseDto
    requestExclude  = {"role"},            // excluded from RequestDto
    baseUrl         = "/api/v1/staff"      // REST controller base path
)
@Table(name = "staff")                     // SQL table name — use @Table when needed
@Entity                                    // required: Spring's entity scanner does not follow meta-annotations
@Getter @Setter @NoArgsConstructor
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

    private String role;
}
```

Build the project. The 5 REST endpoints are immediately available:

| Method | Path | Action |
|--------|------|--------|
| `POST`   | `/api/v1/staff`        | Create a new staff member |
| `GET`    | `/api/v1/staff`        | List all (paginated, sorted by `updatedAt DESC`) |
| `GET`    | `/api/v1/staff/{id}`   | Get by ID |
| `PUT`    | `/api/v1/staff/{id}`   | Update |
| `DELETE` | `/api/v1/staff/{id}`   | Soft delete (sets `deleted = true`) |

**Audit fields** (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deletedBy`, `deleted`) are populated automatically — you never set them manually.

---

## SQL table name

The SQL table name is controlled by `@jakarta.persistence.Table`, not by ata-lib.

If `@Table` is absent, JPA defaults to the class name lowercased. **Always add `@Table(name = "...")` when the class name is a reserved SQL keyword.**

| Class name | Without `@Table` | With `@Table(name = "users")` |
|---|---|---|
| `Staff` | table `staff` — OK | `staff` if you want to be explicit |
| `User` | table `user` — **fails in PostgreSQL** | table `users` — OK |
| `Order` | table `order` — **fails in PostgreSQL** | table `orders` — OK |

---

## UUID entities

For entities that need a UUID primary key, extend `AbstractUuidAuditingEntity` instead:

```java
@AtaEntity(responseExclude = {"password"}, baseUrl = "/api/v1/users")
@Table(name = "users")    // mandatory here: "user" is a reserved word in PostgreSQL
@Entity
@Getter @Setter @NoArgsConstructor
public class User extends AbstractUuidAuditingEntity {

    @Email @NotBlank
    private String email;

    private String password;

    private String name;
}
```

The processor detects the `UUID` id type automatically. It generates:
- `UserResponseDto` with `UUID id`
- `UserRepository extends JpaRepository<User, UUID>`
- `UserController` with `@PathVariable UUID id` on all endpoints

No configuration needed — just extend the right base class.

| Base class | ID type | Generation strategy |
|---|---|---|
| `AbstractAuditingEntity` | `Long` | `GenerationType.IDENTITY` (auto-increment) |
| `AbstractUuidAuditingEntity` | `UUID` | `GenerationType.UUID` (database-generated UUID) |

---

## What AbstractAuditingEntity provides

Both base classes inherit from `AbstractAuditingBase`, which gives you:

| Field | Type | Description |
|---|---|---|
| `createdAt` | `LocalDateTime` | Set on first save, never updated |
| `updatedAt` | `LocalDateTime` | Updated on every save |
| `createdBy` | `String` | Username from `SecurityContextHolder` (falls back to `"SYSTEM"`) |
| `updatedBy` | `String` | Username of last modifier |
| `deletedBy` | `String` | Username who soft-deleted the record |
| `deleted` | `Boolean` | Soft-delete flag — default `false` |

These fields are **always excluded** from both DTOs. You never map them manually.

---

## @AtaEntity attributes

| Attribute | Type | Default | Description |
|---|---|---|---|
| `responseExclude` | `String[]` | `{}` | Fields to exclude from `ResponseDto` |
| `requestExclude` | `String[]` | `{}` | Fields to exclude from `RequestDto` |
| `baseUrl` | `String` | `/{classNameLowercase}` | Controller base URL |

Audit fields (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deletedBy`, `deleted`) and `id` are always excluded from `RequestDto`. Audit fields are always excluded from `ResponseDto` — only `id` appears in it.

---

## Lifecycle hooks

The generated `ServiceImpl` extends `AbstractGenericService`, which exposes hooks you can override to run custom logic at each stage of the CRUD flow:

```
POST  → beforeCreate → [map DTO → entity] → afterMapping → [save] → afterCreate
PUT   → beforeUpdate → [load entity] → [map DTO onto entity] → afterUpdateMapping → [save] → afterUpdate
DELETE → beforeDelete → [soft delete] → afterDelete
```

To add custom logic, create a class that extends the generated `ServiceImpl` and annotate it with `@Primary` so Spring uses it instead:

```java
// Place this in a different package than the entity, or it will be overwritten on rebuild
@Service
@Primary
public class UserService extends UserServiceImpl {

    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository repository, UserMapper mapper,
                       PasswordEncoder passwordEncoder, EmailService emailService) {
        super(repository, mapper);
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // Hash the password before the DTO is mapped to the entity
    @Override
    protected void beforeCreate(UserRequestDto dto) {
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
    }

    // Send a welcome email after the user is saved
    @Override
    protected void afterCreate(User user, UserRequestDto dto) {
        emailService.sendWelcome(user.getEmail());
    }

    // On update, only hash the password if a new one was provided
    @Override
    protected void beforeUpdate(Long id, UserRequestDto dto) {
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
    }

    // Prevent deleting users who are account owners
    @Override
    protected void beforeDelete(Long id) {
        if (userIsAccountOwner(id)) {
            throw new IllegalStateException("Cannot delete an account owner");
        }
    }
}
```

All available hooks:

| Hook | When it runs |
|---|---|
| `beforeCreate(REQUEST_DTO dto)` | Before DTO → entity mapping, on create |
| `afterMapping(ENTITY entity, REQUEST_DTO dto)` | After mapping, before save, on create |
| `afterCreate(ENTITY entity, REQUEST_DTO dto)` | After save, on create |
| `beforeUpdate(ID id, REQUEST_DTO dto)` | Before entity is loaded, on update |
| `afterUpdateMapping(ENTITY entity, REQUEST_DTO dto)` | After partial mapping, before save, on update |
| `afterUpdate(ENTITY entity, REQUEST_DTO dto)` | After save, on update |
| `beforeDelete(ID id)` | Before soft delete |
| `afterDelete(ID id)` | After soft delete |

---

## Custom queries

Override `fetchEntities` to filter the paginated list — by tenant, role, status, or any other criterion:

```java
@Service
@Primary
public class StaffService extends StaffServiceImpl {

    public StaffService(StaffRepository repository, StaffMapper mapper) {
        super(repository, mapper);
    }

    // Only return staff belonging to the current tenant
    @Override
    protected Page<Staff> fetchEntities(Pageable pageable) {
        String tenantId = TenantContext.getCurrent();
        return repository.findAllByDeletedFalseAndTenantId(pageable, tenantId);
    }
}
```

---

## Security

**Option A — Spring Security filter chain (recommended for generated controllers):**

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
            .anyRequest().authenticated()
        );
        return http.build();
    }
}
```

**Option B — `@SecuredCrud` (declarative, per controller):**

Write a manual controller that extends `AbstractGenericController` directly and annotate it with `@SecuredCrud`:

```java
@AtaController("/api/v1/staff")
@SecuredCrud(
    create = {"ROLE_ADMIN"},
    update = {"ROLE_ADMIN"},
    delete = {"ROLE_ADMIN"},
    read   = {"ROLE_USER", "ROLE_ADMIN"},
    list   = {"ROLE_USER", "ROLE_ADMIN"}
)
public class StaffController extends AbstractGenericController<StaffRequestDto, StaffResponseDto, Long> {

    public StaffController(StaffServiceImpl service) {
        super(service);
    }
}
```

`@SecuredCrud` is enforced by `CrudSecurityAspect`. It is registered automatically by the starter when Spring Security is on the classpath. With `ata-lib-core`, register it manually:

```java
@Bean
public CrudSecurityAspect crudSecurityAspect() {
    return new CrudSecurityAspect();
}
```

---

## Starter vs Core

| Situation | Use |
|---|---|
| New Spring Boot project, no existing JPA/AOP setup | `ata-lib-spring-boot-starter` |
| JHipster project | `ata-lib-core` |
| Project that already has `@EnableJpaAuditing` | `ata-lib-core` |
| Full control over bean registration | `ata-lib-core` |

**Rule of thumb**: if your project already has a `DatabaseConfiguration` or any class annotated with `@EnableJpaAuditing`, use `ata-lib-core` to avoid duplicate bean conflicts.

---

## Manual mode (without processor)

If you prefer to write each layer yourself, omit `ata-lib-processor` from `annotationProcessorPaths` and extend the abstractions directly:

```java
// Entity
@Entity
@Getter @Setter @NoArgsConstructor
public class Staff extends AbstractAuditingEntity { ... }

// Repository
@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Page<Staff> findAllByDeletedFalse(Pageable pageable);
    List<Staff> findAllByDeletedFalse();
}

// Service
@AtaService
public class StaffServiceImpl extends AbstractGenericService<Staff, StaffRequestDto, StaffResponseDto, Long> {

    public StaffServiceImpl(StaffRepository repo, StaffMapper mapper) {
        super(repo, mapper::toEntity, mapper::toDto, mapper::updateEntity);
    }

    @Override
    protected Page<Staff> fetchEntities(Pageable pageable) {
        return repository.findAllByDeletedFalse(PageRequest.of(
            pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "updatedAt")
        ));
    }

    @Override
    public List<StaffResponseDto> getAllWithoutPagination() {
        return repository.findAllByDeletedFalse().stream()
            .map(mapper::toDto).collect(Collectors.toList());
    }
}

// Controller
@AtaController("/api/v1/staff")
public class StaffController extends AbstractGenericController<StaffRequestDto, StaffResponseDto, Long> {
    public StaffController(StaffServiceImpl service) { super(service); }
}
```

---

## Known limitations

**`@Entity` must be placed directly on the class**  
Spring Boot's entity scanner does not follow meta-annotations. `@AtaEntity` includes `@Entity` as a meta-annotation but the scanner ignores it. Place `@Entity` directly on every entity class.

**Lombok annotations must be placed directly on the class**  
`@Getter`, `@Setter`, `@NoArgsConstructor`, etc. cannot be inherited through meta-annotations. Place them directly on the entity.

**Do not use `@Builder` on entities extending `AbstractAuditingEntity`**  
Lombok's `@Builder` does not include inherited fields. MapStruct cannot generate mappings for fields it cannot access. Use `@Getter @Setter @NoArgsConstructor` instead. If a builder is required, use `@SuperBuilder` (requires `@SuperBuilder` on the base class too).

**`@Table` is required for reserved SQL keywords**  
`user`, `order`, `group`, and other SQL reserved words will cause a DDL error at startup. Always add `@Table(name = "users")` (or any non-reserved name) when the class name clashes.

---

## Module structure

```
ata-lib-parent
├── ata-lib-core
│   ├── io.atalib.domain       → AbstractAuditingBase, AbstractAuditingEntity (Long), AbstractUuidAuditingEntity (UUID)
│   ├── io.atalib.service      → GenericService interface, AbstractGenericService
│   ├── io.atalib.controller   → AbstractGenericController
│   ├── io.atalib.security     → @SecuredCrud, CrudSecurityAspect
│   ├── io.atalib.annotation   → @AtaEntity, @AtaService, @AtaController, @AtaMapper
│   ├── io.atalib.exception    → EntityNotFoundException
│   └── io.atalib.util         → AuditUtils
├── ata-lib-processor          ← compile-time only, not in runtime classpath
│   └── io.atalib.processor    → AtaEntityProcessor + 6 generators
└── ata-lib-spring-boot-starter
    └── io.atalib.autoconfigure → AtaLibAutoConfiguration (@EnableJpaAuditing, AOP, CrudSecurityAspect)
```

---

## Stack

- Java 17
- Spring Boot 3.3+
- Spring Data JPA
- Spring Security (optional — required only for `@SecuredCrud`)
- MapStruct 1.5+
- Lombok 1.18+
