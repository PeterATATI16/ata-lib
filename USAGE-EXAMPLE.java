// =============================================================================
// GUIDE D'UTILISATION — ata-lib 1.0.0
// =============================================================================
//
// DEUX MODES :
//   • Mode processor (recommandé) — 1 fichier → CRUD complet généré automatiquement
//   • Mode manuel                  — écrire chaque couche soi-même (plus de contrôle)
//
// =============================================================================
// 1. DÉPENDANCES pom.xml
// =============================================================================
//
// Dépendance principale (choisir l'une ou l'autre) :
//
//   Starter (auto-configure JPA auditing + AOP + sécurité) :
//   <dependency>
//       <groupId>io.atalib</groupId>
//       <artifactId>ata-lib-spring-boot-starter</artifactId>
//       <version>1.0.0</version>
//   </dependency>
//
//   Core seul (si le projet configure déjà @EnableJpaAuditing / @EnableAspectJAutoProxy) :
//   <dependency>
//       <groupId>io.atalib</groupId>
//       <artifactId>ata-lib-core</artifactId>
//       <version>1.0.0</version>
//   </dependency>
//
// Annotation processors (maven-compiler-plugin > annotationProcessorPaths) :
// Ordre OBLIGATOIRE : Lombok → MapStruct → ata-lib-processor
//
//   <annotationProcessorPaths>
//       <path><groupId>org.projectlombok</groupId>
//             <artifactId>lombok</artifactId><version>...</version></path>
//       <path><groupId>org.mapstruct</groupId>
//             <artifactId>mapstruct-processor</artifactId><version>...</version></path>
//       <path><groupId>io.atalib</groupId>
//             <artifactId>ata-lib-processor</artifactId><version>1.0.0</version></path>
//   </annotationProcessorPaths>
//
// =============================================================================
// MODE PROCESSOR — 1 fichier par entité, tout le reste est généré
// =============================================================================
//
// 2. ENTITÉ — seul fichier à écrire
// =============================================================================
//
// Note : @Builder n'est pas compatible avec les champs hérités de AbstractAuditingEntity.
// Utiliser @SuperBuilder si un builder est nécessaire, sinon omettre.

@AtaEntity(
        table           = "staff",
        responseExclude = {"password"},                         // exclure du ResponseDto
        requestExclude  = {"id"},                               // exclure du RequestDto
        baseUrl         = "/api/v1/staff"                       // URL du controller généré
)
@Getter
@Setter
@NoArgsConstructor
@Entity                                                         // obligatoire en direct (scanner JPA)
public class Staff extends AbstractAuditingEntity {

    @Column(name = "first_name", length = 100)
    @NotBlank @Size(max = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    @NotBlank @Size(max = 100)
    private String lastName;

    @Column(unique = true, length = 150)
    @Email @NotBlank
    private String email;

    @Column(length = 255)
    private String password;
}

// =============================================================================
// 3. CE QUI EST GÉNÉRÉ AUTOMATIQUEMENT (target/generated-sources/annotations/)
// =============================================================================

// StaffResponseDto.java — id + firstName + lastName + email (password exclu)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffResponseDto {
    private Long id;
    @NotBlank @Size(max = 100)  private String firstName;
    @NotBlank @Size(max = 100)  private String lastName;
    @Email @NotBlank            private String email;
}

// StaffRequestDto.java — firstName + lastName + email + password (id exclu)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffRequestDto {
    @NotBlank @Size(max = 100)  private String firstName;
    @NotBlank @Size(max = 100)  private String lastName;
    @Email @NotBlank            private String email;
    private String password;
}

// StaffMapper.java — interface MapStruct (MapStruct génère l'impl au round suivant)
@Mapper(componentModel = "spring")
public interface StaffMapper {
    StaffResponseDto toDto(Staff entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deleted",   ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    Staff toEntity(StaffRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "createdAt", ignore = true) /* ... autres champs d'audit ... */
    void updateEntity(@MappingTarget Staff entity, StaffRequestDto dto);
}

// StaffRepository.java
@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    List<Staff> findAllByDeletedFalse();
}

// StaffServiceImpl.java — Spring bean @Service + @Transactional via @AtaService
@AtaService
public class StaffServiceImpl
        extends AbstractGenericService<Staff, StaffRequestDto, StaffResponseDto, Long> {

    public StaffServiceImpl(StaffRepository repository, StaffMapper mapper) {
        super(repository, mapper::toEntity, mapper::toDto, mapper::updateEntity);
    }

    @Override
    public List<StaffResponseDto> getAllWithoutPagination() {
        return repository.findAllByDeletedFalse().stream().map(mapper::toDto).toList();
    }
}

// StaffController.java — Spring bean @RestController @RequestMapping via @AtaController
@AtaController("/api/v1/staff")
public class StaffController
        extends AbstractGenericController<StaffRequestDto, StaffResponseDto, Long> {

    public StaffController(StaffServiceImpl service) {
        super(service);
    }
}

// RÉSULTAT : 5 endpoints REST disponibles sans rien écrire de plus :
//   POST   /api/v1/staff        → create
//   PUT    /api/v1/staff/{id}   → update
//   GET    /api/v1/staff/{id}   → getById
//   GET    /api/v1/staff        → getAll paginé
//   DELETE /api/v1/staff/{id}   → soft delete

// =============================================================================
// 4. CUSTOMISATION — overrider uniquement ce qui change
// =============================================================================

// Logique métier custom dans le service :
// Étendre StaffServiceImpl (généré) dans un package différent + @Primary
// com.example.service.StaffService (pas com.example.domain)
@Service
@Primary
public class StaffService extends StaffServiceImpl {

    public StaffService(StaffRepository repository, StaffMapper mapper) {
        super(repository, mapper);
    }

    @Override
    protected void beforeCreate(StaffRequestDto dto) {
        // valider l'unicité de l'email, hasher le password, etc.
    }
}

// Sécurité sur les endpoints générés — configurer Spring Security globalement :
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

// Endpoints custom ou @SecuredCrud : écrire un controller manuel
// (voir sections 8-9 ci-dessous — mode manuel)
// Le controller généré (StaffController) doit être exclu du component scan.

// =============================================================================
// MODE MANUEL — sans le processor, écrire chaque couche (cf. sections 2-8 ci-dessous)
// =============================================================================
//
// Utile quand :
//   • l'entité a des types complexes (héritage multi-niveaux, discriminator, etc.)
//   • les DTOs ont une structure très différente de l'entité
//   • le service a une logique de pagination ou de filtrage très custom
//
// 5. ENTITÉ (mode manuel)
// =============================================================================

@Entity
@Table(name = "staff")
@Getter @Setter @NoArgsConstructor
public class Staff extends AbstractAuditingEntity {

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(unique = true, length = 150)
    private String email;
}

// =============================================================================
// 6. REPOSITORY (mode manuel)
// =============================================================================

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Page<Staff> findAllByDeletedFalse(Pageable pageable);
    List<Staff> findAllByDeletedFalse();
}

// =============================================================================
// 7. MAPPER (mode manuel)
// =============================================================================

@Mapper(componentModel = "spring")
public interface StaffMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted",   ignore = true)
    Staff toEntity(StaffRequestDto dto);

    StaffResponseDto toDto(Staff staff);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    @Mapping(target = "deleted",   ignore = true)
    void updateEntity(@MappingTarget Staff entity, StaffRequestDto dto);
}

// =============================================================================
// 8. SERVICE (mode manuel)
// =============================================================================

@AtaService
public class StaffServiceImpl
        extends AbstractGenericService<Staff, StaffRequestDto, StaffResponseDto, Long> {

    public StaffServiceImpl(StaffRepository repo, StaffMapper mapper) {
        super(repo, mapper::toEntity, mapper::toDto, mapper::updateEntity);
    }

    @Override
    protected Page<Staff> fetchEntities(Pageable pageable) {
        return ((StaffRepository) repository).findAllByDeletedFalse(pageable);
    }

    @Override
    public List<StaffResponseDto> getAllWithoutPagination() {
        return ((StaffRepository) repository).findAllByDeletedFalse()
                .stream().map(entityToDtoMapper).toList();
    }
}

// =============================================================================
// 9. CONTRÔLEUR (mode manuel)
// =============================================================================

@AtaController("/api/v1/staff")
@SecuredCrud(
        create = {"ADMIN"},
        update = {"ADMIN"},
        delete = {"ADMIN"}
)
public class StaffController
        extends AbstractGenericController<StaffRequestDto, StaffResponseDto, Long> {

    public StaffController(StaffServiceImpl service) {
        super(service);
    }
}

// =============================================================================
// LIQUIBASE — champs à inclure dans votre changelog
// =============================================================================
//   id            BIGINT PRIMARY KEY AUTO_INCREMENT
//   first_name    VARCHAR(100)
//   last_name     VARCHAR(100)
//   email         VARCHAR(150) UNIQUE
//   created_at    TIMESTAMP
//   updated_at    TIMESTAMP
//   created_by    VARCHAR(50)
//   updated_by    VARCHAR(50)
//   deleted_by    VARCHAR(50)
//   deleted       BOOLEAN NOT NULL DEFAULT FALSE
// =============================================================================
