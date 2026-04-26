# ata-lib — Spécification Technique et Fonctionnelle

**Version** : 1.0.1  
**Stack** : Java 17 · Spring Boot 3.3+ · Spring Data JPA · MapStruct · Lombok  
**Dépôt** : https://github.com/PeterATATI16/ata-lib

---

## 1. Objectif

ata-lib est une librairie Java extractant les patterns CRUD génériques communs à tous les microservices Spring Boot du projet. Elle élimine le boilerplate répété (entité auditée, service CRUD, contrôleur REST, sécurité déclarative) en fournissant des classes abstraites et des annotations composées réutilisables.

**Problème résolu** : sans ata-lib, chaque nouveau microservice réimplémente les mêmes ~300 lignes de code (GenericService, AbstractGenericService, AbstractGenericController, CrudSecurityAspect, BaseEntity) avec les mêmes risques d'incohérence.

---

## 2. Architecture des modules

```
ata-lib-parent (POM)
├── ata-lib-core
│   Contient toutes les abstractions sans aucune auto-configuration.
│   Utilisable dans tout projet Spring Boot existant.
│
└── ata-lib-spring-boot-starter
    Dépend de ata-lib-core.
    Active automatiquement JPA Auditing, AspectJ et CrudSecurityAspect
    via Spring Boot Auto-Configuration.
```

### 2.1 Dépendances de ata-lib-core

| Dépendance | Scope | Motif |
|---|---|---|
| spring-boot-starter-data-jpa | compile | AbstractAuditingEntity, JpaRepository |
| spring-boot-starter-web | compile | AbstractGenericController |
| spring-boot-starter-validation | compile | @Valid sur les endpoints |
| spring-boot-starter-aop | compile | CrudSecurityAspect |
| spring-boot-starter-security | **optional** | CrudSecurityAspect (pas chargé si absent) |
| mapstruct | compile | Annotations @Mapper sur StaffMapper |
| lombok | **optional** | AbstractAuditingEntity uniquement |

### 2.2 Dépendances de ata-lib-spring-boot-starter

| Dépendance | Scope | Motif |
|---|---|---|
| ata-lib-core | compile | Re-exporte toutes les classes |
| spring-boot-autoconfigure | compile | @AutoConfiguration, @ConditionalOn* |

---

## 3. Composants — Spécification fonctionnelle

### 3.1 AbstractAuditingEntity

**Package** : `io.atalib.domain`  
**Type** : `@MappedSuperclass` abstract  

Classe de base pour toutes les entités JPA du projet. Fournit l'identifiant, les champs d'audit et le mécanisme de soft delete.

**Champs**

| Champ | Type | Colonne | Description |
|---|---|---|---|
| `id` | `Long` | `id` | Clé primaire auto-incrémentée |
| `createdAt` | `LocalDateTime` | `created_at` | Date de création (non modifiable) |
| `updatedAt` | `LocalDateTime` | `updated_at` | Date de dernière modification |
| `createdBy` | `String` | `created_by` | Login de l'auteur de création |
| `updatedBy` | `String` | `updated_by` | Login du dernier modificateur |
| `deletedBy` | `String` | `deleted_by` | Login du suppresseur (soft delete) |
| `deleted` | `Boolean` | `deleted` | Marqueur de suppression logique (défaut : false) |

**Hooks JPA**

| Hook | Déclencheur | Comportement |
|---|---|---|
| `@PrePersist` | Avant INSERT | Initialise createdAt, updatedAt, createdBy, updatedBy. Appelle `beforePersist()`. |
| `@PreUpdate` | Avant UPDATE | Met à jour updatedAt, updatedBy. Appelle `beforePersist()`. |
| `@PreRemove` | Avant DELETE physique | Marque deleted=true, renseigne deletedBy. |

**Méthode `softDelete()`** : positionne deleted=true, deletedBy et updatedAt sans supprimer la ligne.  
**Hook `beforePersist()`** : méthode vide overridable dans les entités filles pour logique custom avant sauvegarde.

**Métamodèle JPA** : `AbstractAuditingEntity_` est inclus dans ata-lib-core pour permettre la génération du métamodèle des entités filles par `hibernate-jpamodelgen`.

---

### 3.2 GenericService\<REQ, RES, ID>

**Package** : `io.atalib.service`  
**Type** : interface

Contrat de service CRUD générique.

| Méthode | Signature | Description |
|---|---|---|
| `create` | `RES create(REQ dto)` | Crée une entité à partir du DTO |
| `update` | `RES update(ID id, REQ dto)` | Met à jour partiellement une entité |
| `get` | `RES get(ID id)` | Récupère une entité par ID |
| `getAll` | `Page<RES> getAll(Pageable p)` | Liste paginée |
| `delete` | `void delete(ID id)` | Suppression (soft si extends AbstractAuditingEntity) |
| `getAllWithoutPagination` | `List<RES> getAllWithoutPagination()` | Liste complète sans pagination — **à implémenter** |

---

### 3.3 AbstractGenericService\<E, REQ, RES, ID>

**Package** : `io.atalib.service`  
**Type** : abstract class implements GenericService

Implémentation CRUD avec pattern Template Method. Injecte les fonctions mapper via constructeur pour éviter la dépendance directe à MapStruct.

**Constructeur**

```java
protected AbstractGenericService(
    JpaRepository<ENTITY, ID> repository,
    Function<REQUEST_DTO, ENTITY> dtoToEntityMapper,
    Function<ENTITY, RESPONSE_DTO> entityToDtoMapper,
    BiConsumer<ENTITY, REQUEST_DTO> updateEntityMapper
)
```

**Comportement de `delete()`** : si l'entité est une instance de `AbstractAuditingEntity`, appelle `softDelete()` puis sauvegarde. Sinon, suppression physique (`repository.delete()`).

**`fetchEntities(Pageable)`** : retourne par défaut toutes les entités triées par `updatedAt DESC`. Override pour filtrer (ex: `findAllByDeletedFalse`).

**Hooks lifecycle disponibles**

```
beforeCreate → afterMapping → [save] → afterCreate
beforeUpdate → afterUpdateMapping → [save] → afterUpdate
beforeDelete → [delete] → afterDelete
```

**`getAllWithoutPagination()`** : méthode abstraite — doit être implémentée dans chaque `ServiceImpl`.

---

### 3.4 AbstractGenericController\<REQ, RES, ID>

**Package** : `io.atalib.controller`  
**Type** : abstract class

Expose les 5 endpoints REST standard. Délègue au `GenericService`.

| HTTP | Path | Méthode service | Code retour |
|---|---|---|---|
| POST | `/` | `service.create(dto)` | 201 Created |
| PUT | `/{id}` | `service.update(id, dto)` | 200 OK |
| GET | `/{id}` | `service.get(id)` | 200 OK |
| GET | `/` | `service.getAll(pageable)` | 200 OK |
| DELETE | `/{id}` | `service.delete(id)` | 204 No Content |

`@Valid` appliqué sur les méthodes POST et PUT — les contraintes des DTOs (`@NotBlank`, `@Email`, etc.) sont validées automatiquement.

---

### 3.5 SecuredCrud

**Package** : `io.atalib.security`  
**Type** : annotation (`@Target(TYPE)`)

Annotation déclarative de sécurité CRUD. Placée sur un contrôleur, elle contrôle l'accès aux 5 endpoints par rôles et/ou permissions.

**Attributs**

| Attribut | Description | Exemple |
|---|---|---|
| `create[]` | Rôles autorisés pour POST | `{"ADMIN"}` |
| `createPermissions[]` | Permissions pour POST | `{"CREER_STAFF"}` |
| `update[]` | Rôles pour PUT | `{"ADMIN", "MANAGER"}` |
| `updatePermissions[]` | Permissions pour PUT | `{"MODIFIER_STAFF"}` |
| `read[]` | Rôles pour GET/{id} | |
| `readPermissions[]` | Permissions pour GET/{id} | |
| `list[]` | Rôles pour GET (liste) | |
| `listPermissions[]` | Permissions pour GET (liste) | |
| `delete[]` | Rôles pour DELETE | `{"ADMIN"}` |
| `deletePermissions[]` | Permissions pour DELETE | |

**Règle de vérification** (dans CrudSecurityAspect) :
- Si l'attribut est vide → accès non restreint pour cette opération
- Rôles vérifiés avec le préfixe `ROLE_` automatiquement ajouté
- Permissions vérifiées directement sur `getAuthority()`
- Si rôles ET permissions sont définis → l'utilisateur doit satisfaire **au moins un** des deux

---

### 3.6 CrudSecurityAspect

**Package** : `io.atalib.security`  
**Type** : `@Aspect` (pas de `@Component` — enregistré via configuration)

Intercepte les méthodes de `AbstractGenericController` avant exécution. Lit l'annotation `@SecuredCrud` sur la classe contrôleur et vérifie les droits via `SecurityContextHolder`.

**Pointcuts**

| Pointcut | Méthode interceptée |
|---|---|
| `@Before create` | `AbstractGenericController.create()` |
| `@Before update` | `AbstractGenericController.update()` |
| `@Before getById` | `AbstractGenericController.getById()` |
| `@Before getAll` | `AbstractGenericController.getAll()` |
| `@Before delete` | `AbstractGenericController.delete()` |

**Comportement** : lève `AccessDeniedException` si l'utilisateur ne satisfait pas les contraintes. Si `SecurityContextHolder` n'a pas d'authentification active, lève également `AccessDeniedException`.

**Activation** :
- Via starter : bean créé automatiquement si `spring-security-core` est sur le classpath (`@ConditionalOnClass`)
- Via core seul : bean à déclarer manuellement dans une `@Configuration`

---

### 3.7 Annotations composées

**Package** : `io.atalib.annotation`

| Annotation | Compose | Mécanisme | Fonctionne ? |
|---|---|---|---|
| `@AtaService` | `@Service + @Transactional` | Spring meta-annotation (runtime) | ✅ |
| `@AtaController(value/path)` | `@RestController + @RequestMapping` avec `@AliasFor` | Spring meta-annotation (runtime) | ✅ |
| `@AtaEntity` | `@Entity + @EntityListeners(AuditingEntityListener.class)` | Spring/Hibernate meta-annotation | ⚠️ voir limitations |
| `@AtaMapper` | `@Mapper(componentModel = "spring")` | APT (compile-time) | ⚠️ voir limitations |

---

## 4. Starter — Auto-configuration

**Classe** : `io.atalib.autoconfigure.AtaLibAutoConfiguration`  
**Fichier de déclaration** : `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

Active automatiquement :

| Configuration | Condition |
|---|---|
| `@EnableJpaAuditing` | Toujours (nécessaire pour `@CreatedDate` / `@LastModifiedDate`) |
| `@EnableAspectJAutoProxy` | Toujours (nécessaire pour CrudSecurityAspect) |
| Bean `CrudSecurityAspect` | Seulement si `org.springframework.security.core.Authentication` est sur le classpath |

---

## 5. Décision : Starter ou Core ?

### Utiliser `ata-lib-spring-boot-starter` si

- Le projet est créé **from scratch** (sans configuration JPA/AOP préexistante)
- Aucune classe n'est annotée `@EnableJpaAuditing` dans le projet
- Aucune classe n'est annotée `@EnableAspectJAutoProxy` dans le projet
- L'objectif est le minimum de configuration

### Utiliser `ata-lib-core` seul si

- Le projet est **JHipster v8** (a son propre `DatabaseConfiguration` avec `@EnableJpaAuditing`)
- Le projet a déjà une configuration `@EnableJpaAuditing(auditorAwareRef = "...")` — ajouter le starter provoquerait un conflit de bean `JpaAuditingRegistrar`
- Le projet a déjà `@EnableAspectJAutoProxy` déclaré
- L'équipe veut un contrôle explicite sur les beans Spring enregistrés

### Configuration complémentaire requise avec core seul

```java
// 1. Bean CrudSecurityAspect (si @SecuredCrud est utilisé)
@Configuration
public class AtaLibConfiguration {
    @Bean
    public CrudSecurityAspect crudSecurityAspect() {
        return new CrudSecurityAspect();
    }
}

// 2. Étendre @EntityScan
@EntityScan(basePackages = { "packages.existants", "com.yourapp.domain" })

// 3. Étendre @EnableJpaRepositories
@EnableJpaRepositories({ "repositories.existantes", "com.yourapp.repository" })
```

---

## 6. Limitations connues

### 6.1 Lombok et méta-annotations

Lombok traite ses annotations (`@Getter`, `@Setter`, `@Builder`, etc.) uniquement lorsqu'elles sont **directement** sur l'élément cible. L'annotation processor Lombok utilise `roundEnv.getElementsAnnotatedWith(Getter.class)` qui n'explore pas les méta-annotations.

**Conséquence** : `@AtaEntity` ne peut pas inclure `@Getter @Setter` de façon transparente. Ces annotations restent obligatoires directement sur la classe entité.

**Contournement futur** : module `ata-lib-processor` (APT) qui génèrerait le code via manipulation de l'AST au moment de la compilation — identique au fonctionnement interne de Lombok.

### 6.2 MapStruct et méta-annotations

MapStruct's annotation processor utilise `roundEnv.getElementsAnnotatedWith(Mapper.class)`. Seules les interfaces annotées **directement** avec `@Mapper` sont détectées.

**Conséquence** : `@AtaMapper` est déclaré dans la librairie mais MapStruct ne génère pas l'implémentation si on l'utilise seul. Utiliser `@Mapper(componentModel = "spring")` directement.

### 6.3 @Entity et entity scan

`LocalContainerEntityManagerFactoryBean` (Spring) utilise `AnnotationTypeFilter(Entity.class, false)` — le paramètre `false` désactive la recherche via méta-annotations.

**Conséquence** : `@AtaEntity` seul ne suffit pas pour qu'Hibernate détecte l'entité. `@Entity` doit être présent directement sur la classe.

### 6.4 @Builder sur les entités JPA

Lombok's `@Builder` sur une sous-classe génère un builder uniquement pour les champs propres à cette classe (pas les champs hérités). MapStruct détecte le builder et tente d'y accéder, ce qui provoque une erreur de compilation pour les champs hérités (`Unknown property "id" in StaffBuilder`).

**Solution** : ne pas utiliser `@Builder` sur les entités JPA. Utiliser `@Getter @Setter @NoArgsConstructor` uniquement. Si un builder est nécessaire, utiliser `@SuperBuilder` de Lombok (implique d'annoter aussi `AbstractAuditingEntity` avec `@SuperBuilder`).

---

## 7. Évolutions prévues

| Évolution | Description | Complexité |
|---|---|---|
| `ata-lib-processor` | Module APT qui génère `extends AbstractAuditingEntity` à la compilation via `@AtaEntity` | Haute |
| `@SuperBuilder` sur AbstractAuditingEntity | Permet d'utiliser `@SuperBuilder` sur les entités filles | Faible |
| Support `UUID` comme ID | Paramétrer le type d'ID dans AbstractAuditingEntity | Faible |
| `BaseFilter` générique | Classe de filtre de recherche avec `Specification<E>` | Moyenne |
| Support multi-tenant | Champ `tenantId` dans AbstractAuditingEntity + filtre automatique | Haute |
