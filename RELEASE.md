## ata-lib v1.0.1 — First stable release

Annotate an entity. Get a complete REST API.

`@AtaEntity` triggers a compile-time annotation processor that generates 6 files automatically — DTOs, MapStruct mapper, repository, service, and controller — with zero runtime overhead and no reflection.

---

### What gets generated

```java
@AtaEntity(responseExclude = {"password"}, baseUrl = "/api/v1/users")
@Table(name = "users")
@Entity
@Getter @Setter @NoArgsConstructor
public class User extends AbstractAuditingEntity {
    @Email @NotBlank private String email;
    private String password;
    private String name;
}
```

On every build, the processor generates:

| File | Content |
|---|---|
| `UserResponseDto` | `id` + fields, `password` excluded |
| `UserRequestDto` | Fields only, `id` and audit fields excluded |
| `UserMapper` | MapStruct — `toDto`, `toEntity`, `updateEntity` |
| `UserRepository` | `JpaRepository<User, Long>` + soft-delete queries |
| `UserServiceImpl` | Full CRUD with lifecycle hooks |
| `UserController` | 5 REST endpoints, ready to use |

**5 endpoints. 0 boilerplate written.**

---

### Highlights

- **Auto-detected ID type** — extend `AbstractAuditingEntity` for `Long` or `AbstractUuidAuditingEntity` for `UUID`. The processor reads the `@Id` field type from the class hierarchy automatically.
- **Lifecycle hooks** — override `beforeCreate`, `afterCreate`, `beforeUpdate`, `beforeDelete`, etc. to add custom logic without touching generated code.
- **Soft delete** — `DELETE /{id}` sets `deleted = true` and records `deletedBy`. Physical deletion never happens.
- **Automatic auditing** — `createdAt`, `updatedAt`, `createdBy`, `updatedBy` populated from `SecurityContextHolder` (falls back to `"SYSTEM"` when Spring Security is not on the classpath).
- **Validation mirroring** — `@NotBlank`, `@Email`, `@Size`, etc. are copied from entity fields onto generated DTOs automatically.

---

### Requirements

- Java 17+
- Spring Boot 3.3+
- Lombok 1.18+
- MapStruct 1.5+

---

### Installation

Add the JitPack repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Then add the dependency:

```xml
<!-- New project (no existing JPA/AOP config) -->
<dependency>
    <groupId>com.github.PeterATATI16.ata-lib</groupId>
    <artifactId>ata-lib-spring-boot-starter</artifactId>
    <version>v1.0.1</version>
</dependency>

<!-- Existing project (JHipster, custom @EnableJpaAuditing, etc.) -->
<dependency>
    <groupId>com.github.PeterATATI16.ata-lib</groupId>
    <artifactId>ata-lib-core</artifactId>
    <version>v1.0.1</version>
</dependency>
```

See the [README](https://github.com/PeterATATI16/ata-lib#readme) for the full setup guide including annotation processor configuration.
