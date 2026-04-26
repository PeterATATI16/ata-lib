// =============================================================================
// GUIDE D'UTILISATION — ata-lib 1.0.0
// =============================================================================
//
// 1. AJOUTER LA DÉPENDANCE DANS VOTRE pom.xml
// =============================================================================
//
// <dependency>
//     <groupId>io.atalib</groupId>
//     <artifactId>ata-lib-core</artifactId>
//     <version>1.0.0</version>
// </dependency>
//
// Ajouter aussi Lombok (si pas déjà présent) :
// <dependency>
//     <groupId>org.projectlombok</groupId>
//     <artifactId>lombok</artifactId>
//     <optional>true</optional>
// </dependency>
//
// Et dans maven-compiler-plugin > annotationProcessorPaths (Lombok AVANT MapStruct) :
// <path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></path>
// <path><groupId>org.mapstruct</groupId><artifactId>mapstruct-processor</artifactId></path>
//
// =============================================================================
// 2. CONFIGURATION SPRING (à faire une seule fois par projet)
// =============================================================================
//
// Si votre projet n'a PAS déjà @EnableJpaAuditing et @EnableAspectJAutoProxy,
// utilisez le starter qui les active automatiquement :
//
// <dependency>
//     <groupId>io.atalib</groupId>
//     <artifactId>ata-lib-spring-boot-starter</artifactId>
//     <version>1.0.0</version>
// </dependency>
//
// Si votre projet a DÉJÀ ces configurations (JHipster, etc.), utilisez uniquement
// ata-lib-core et enregistrez CrudSecurityAspect manuellement :
//
// @Configuration
// public class AtaLibConfiguration {
//     @Bean public CrudSecurityAspect crudSecurityAspect() {
//         return new CrudSecurityAspect();
//     }
// }
//
// Aussi ajouter vos packages à @EntityScan et @EnableJpaRepositories :
// @EntityScan(basePackages = { "com.existing.entity", "com.yourapp.domain" })
// @EnableJpaRepositories({ "com.existing.repository", "com.yourapp.repository" })
//
// =============================================================================
// 3. ENTITÉ — extends AbstractAuditingEntity
// =============================================================================
//
// Note : pas de @Builder sur les entités JPA — le Builder Lombok n'inclut pas
// les champs hérités, ce qui cause des conflits avec MapStruct.

@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
public class Staff extends AbstractAuditingEntity {   // ← AbstractAuditingEntity d'ata-lib

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", length = 150, unique = true)
    private String email;
}

// Champs hérités automatiquement via AbstractAuditingEntity :
//   id, createdAt, updatedAt, createdBy, updatedBy, deletedBy, deleted

// =============================================================================
// 4. DTOs — POJOs avec validation
// =============================================================================

public class StaffRequestDto {
    @NotBlank @Size(max = 100) private String firstName;
    @NotBlank @Size(max = 100) private String lastName;
    @Email @NotBlank @Size(max = 150) private String email;
    // getters / setters
}

public class StaffResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    // getters / setters
}

// =============================================================================
// 5. MAPPER — @Mapper(componentModel = "spring") + ignorer les champs hérités
// =============================================================================

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

// =============================================================================
// 6. REPOSITORY — JpaRepository standard
// =============================================================================

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Page<Staff> findAllByDeletedFalse(Pageable pageable);
}

// =============================================================================
// 7. SERVICE — @AtaService + extends AbstractGenericService
// =============================================================================

public interface StaffService
        extends GenericService<StaffRequestDto, StaffResponseDto, Long> {
}

@AtaService                                        // ← @Service + @Transactional inclus
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

// =============================================================================
// 8. CONTRÔLEUR — @AtaController + extends AbstractGenericController + @SecuredCrud
// =============================================================================

@AtaController("/api/v1/staff")                    // ← @RestController + @RequestMapping inclus
@Tag(name = "Staff")
@SecuredCrud(
    create = {"ADMIN"},
    update = {"ADMIN"},
    delete = {"ADMIN"}
    // read et list : accessibles à tous les utilisateurs authentifiés (pas de restriction)
)
public class StaffController
        extends AbstractGenericController<StaffRequestDto, StaffResponseDto, Long> {

    public StaffController(StaffService service) {
        super(service);
    }
}

// =============================================================================
// RÉSULTAT : 8 fichiers, 0 boilerplate répété.
// Les 5 endpoints REST sont disponibles automatiquement :
//   POST   /api/v1/staff          → create   (ADMIN seulement)
//   PUT    /api/v1/staff/{id}     → update   (ADMIN seulement)
//   GET    /api/v1/staff/{id}     → getById
//   GET    /api/v1/staff?page=0   → getAll paginé
//   DELETE /api/v1/staff/{id}     → soft delete (ADMIN seulement)
//
// ANNOTATIONS ata-lib disponibles :
//   @AtaService    = @Service + @Transactional
//   @AtaController = @RestController + @RequestMapping
//   @SecuredCrud   = sécurité déclarative par rôles / permissions
//   @AtaEntity     = @Entity + @EntityListeners (sucre syntaxique, @Entity reste obligatoire)
//
// LIQUIBASE — champs à ajouter dans votre changelog :
//   id BIGINT PK AUTOINCREMENT
//   first_name, last_name, email (champs métier)
//   created_at, updated_at TIMESTAMP
//   created_by, updated_by, deleted_by VARCHAR(50)
//   deleted BOOLEAN DEFAULT FALSE NOT NULL
// =============================================================================
