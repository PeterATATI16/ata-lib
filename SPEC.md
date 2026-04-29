# ata-lib — Spécification Technique et Fonctionnelle

**Version** : 1.1.0    
**Stack** : Java 17 · Spring Boot 3.3+ · Spring Data JPA · MapStruct · Lombok  
**Dépôt** : https://github.com/PeterATATI16/ata-lib

---

## 1. Objectif

ata-lib est une librairie Java extractant les patterns CRUD génériques communs à tous les microservices Spring Boot du projet. Elle élimine le boilerplate répété en fournissant :

- Des classes abstraites et annotations composées réutilisables (`ata-lib-core`)
- Un auto-configuration Spring Boot optionnelle (`ata-lib-spring-boot-starter`)
- Un processeur d'annotation compile-time qui génère automatiquement tous les fichiers companion d'une entité (`ata-lib-processor`)

**Avant** : chaque nouveau microservice réimplémentait ~300 lignes (entité, DTOs, mapper, repository, service, contrôleur).  
**Après** : une entité annotée `@AtaEntity` génère les 6 fichiers automatiquement à la compilation.

---

## 2. Architecture des modules

```
ata-lib-parent (POM)
├── ata-lib-core
│   Contient toutes les abstractions sans aucune auto-configuration.
│   Utilisable dans tout projet Spring Boot existant.
│
├── ata-lib-processor
│   Processeur d'annotation (APT) déclenché par @AtaEntity.
│   Génère : ResponseDto, RequestDto, Mapper, Repository, ServiceImpl, Controller.
│   Aucune dépendance Spring requise — utilise uniquement les APIs javax.lang.model.
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
| mapstruct | compile | Annotations @Mapper |
| lombok | **optional** | AbstractAuditingEntity uniquement |

### 2.2 Dépendances de ata-lib-processor

| Dépendance | Scope | Motif |
|---|---|---|
| *(aucune)* | — | Uniquement les APIs JDK standard (javax.annotation.processing, javax.lang.model) |

Le processeur est déclaré en `annotationProcessorPaths` dans le projet consommateur — il n'entre pas dans le classpath de runtime.

### 2.3 Dépendances de ata-lib-spring-boot-starter

| Dépendance | Scope | Motif |
|---|---|---|
| ata-lib-core | compile | Re-exporte toutes les classes |
| spring-boot-autoconfigure | compile | @AutoConfiguration, @ConditionalOn* |

---

## 3. Composants — Spécification fonctionnelle

### 3.1 AbstractAuditingEntity

**Package** : `io.atalib.domain`  
**Type** : `@MappedSuperclass` abstract

Classe de base pour toutes les entités JPA du projet.

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

**`softDelete()`** : positionne deleted=true, deletedBy et updatedAt sans supprimer la ligne.  
**`beforePersist()`** : méthode vide overridable dans les entités filles.

---

### 3.2 GenericService\<REQ, RES, ID>

**Package** : `io.atalib.service`  
**Type** : interface

| Méthode | Signature | Description |
|---|---|---|
| `create` | `RES create(REQ dto)` | Crée une entité |
| `update` | `RES update(ID id, REQ dto)` | Met à jour partiellement |
| `get` | `RES get(ID id)` | Récupère par ID |
| `getAll` | `Page<RES> getAll(Pageable p)` | Liste paginée |
| `delete` | `void delete(ID id)` | Suppression (soft si extends AbstractAuditingEntity) |
| `getAllWithoutPagination` | `List<RES> getAllWithoutPagination()` | Liste complète — à implémenter |

---

### 3.3 AbstractGenericService\<E, REQ, RES, ID>

**Package** : `io.atalib.service`  
**Type** : abstract class implements GenericService

Implémentation CRUD avec pattern Template Method. Injecte les fonctions mapper via constructeur.

**Constructeur**

```java
protected AbstractGenericService(
    JpaRepository<ENTITY, ID> repository,
    Function<REQUEST_DTO, ENTITY> dtoToEntityMapper,
    Function<ENTITY, RESPONSE_DTO> entityToDtoMapper,
    BiConsumer<ENTITY, REQUEST_DTO> updateEntityMapper
)
```

**Comportement de `delete()`** : si l'entité est une `AbstractAuditingEntity`, appelle `softDelete()` puis sauvegarde. Sinon, suppression physique.

**`fetchEntities(Pageable)`** : retourne toutes les entités triées par `updatedAt DESC`. Override pour filtrer.

**Hooks lifecycle**

```
beforeCreate → afterMapping → [save] → afterCreate
beforeUpdate → afterUpdateMapping → [save] → afterUpdate
beforeDelete → [delete] → afterDelete
```

**`getAllWithoutPagination()`** : méthode abstraite — implémentée dans le ServiceImpl généré via `findAllByDeletedFalse()`.

---

### 3.4 AbstractGenericController\<REQ, RES, ID>

**Package** : `io.atalib.controller`  
**Type** : abstract class

| HTTP | Path | Méthode service | Code retour |
|---|---|---|---|
| POST | `/` | `service.create(dto)` | 201 Created |
| PUT | `/{id}` | `service.update(id, dto)` | 200 OK |
| GET | `/{id}` | `service.get(id)` | 200 OK |
| GET | `/` | `service.getAll(pageable)` | 200 OK |
| DELETE | `/{id}` | `service.delete(id)` | 204 No Content |

`@Valid` appliqué sur POST et PUT.

---

### 3.5 SecuredCrud

**Package** : `io.atalib.security`  
**Type** : annotation (`@Target(TYPE)`)

Annotation déclarative de sécurité CRUD. À placer sur le contrôleur.

**Attributs**

| Attribut | Description |
|---|---|
| `create[]` / `createPermissions[]` | Rôles / permissions pour POST |
| `update[]` / `updatePermissions[]` | Rôles / permissions pour PUT |
| `read[]` / `readPermissions[]` | Rôles / permissions pour GET/{id} |
| `list[]` / `listPermissions[]` | Rôles / permissions pour GET (liste) |
| `delete[]` / `deletePermissions[]` | Rôles / permissions pour DELETE |

**Règle** : attribut vide = accès non restreint. Rôles et permissions sont évalués en OR.

---

### 3.6 CrudSecurityAspect

**Package** : `io.atalib.security`  
**Type** : `@Aspect`

Intercepte les méthodes de `AbstractGenericController` via `@Before` pointcuts. Lit `@SecuredCrud` et vérifie les droits via `SecurityContextHolder`. Lève `AccessDeniedException` si l'utilisateur ne satisfait pas les contraintes.

**Activation** : automatique via starter (si Spring Security sur le classpath) ou bean manuel via `@Configuration`.

---

### 3.7 Annotations composées

**Package** : `io.atalib.annotation`

| Annotation | Compose |
|---|---|
| `@AtaEntity(table, responseExclude, requestExclude, baseUrl)` | `@Entity + @EntityListeners(AuditingEntityListener.class)` + déclenche le processor |
| `@AtaService` | `@Service + @Transactional` |
| `@AtaController(value/path)` | `@RestController + @RequestMapping` |

**Attributs de @AtaEntity**

| Attribut | Type | Défaut | Description |
|---|---|---|---|
| `table` | `String` | snake_case du nom de classe | Nom de la table SQL |
| `responseExclude` | `String[]` | `{}` | Champs à exclure du ResponseDto (champs d'audit toujours exclus) |
| `requestExclude` | `String[]` | `{}` | Champs à exclure du RequestDto (id + champs d'audit toujours exclus) |
| `baseUrl` | `String` | `/{nomEntitéMinuscules}` | Base URL du contrôleur généré |

---

### 3.8 AtaEntityProcessor

**Package** : `io.atalib.processor`  
**Type** : `AbstractProcessor` (APT standard Java)  
**Déclenché par** : `@AtaEntity` sur une classe

Génère 6 fichiers source dans le même package que l'entité, lors de la compilation (round 1 de l'APT). MapStruct traite ensuite l'interface mapper générée au round suivant.

**Fichiers générés**

| Fichier | Contenu |
|---|---|
| `{Entity}ResponseDto` | Champs de l'entité + `id`, hors `responseExclude` et champs d'audit |
| `{Entity}RequestDto` | Champs de l'entité, hors `requestExclude`, `id` et champs d'audit |
| `{Entity}Mapper` | Interface `@Mapper(componentModel = "spring")` avec `toDto`, `toEntity`, `updateEntity` |
| `{Entity}Repository` | Interface `JpaRepository<Entity, Long>` + `findAllByDeletedFalse()` |
| `{Entity}ServiceImpl` | Extends `AbstractGenericService`, `@AtaService`, implémente `getAllWithoutPagination()` |
| `{Entity}Controller` | Extends `AbstractGenericController`, `@AtaController(baseUrl)` |

**Mirroring des annotations** : les annotations de validation (`@NotBlank`, `@Email`, `@Size`, etc.) présentes sur les champs de l'entité sont automatiquement copiées sur les champs correspondants des DTOs générés. Les annotations JPA (`jakarta.persistence.*`), Lombok et Spring Data sont exclues du mirroring.

**Gestion des types** : les types de champs sont résolus vers leur nom simple avec imports appropriés. Les types `java.lang.*` et les primitifs ne génèrent pas d'import.

**Lecture des valeurs d'annotation** : le processeur utilise l'API `AnnotationMirror` (pas de chargement de classe `AtaEntity.class`) pour éviter toute dépendance au classpath du processeur.

**Champs d'audit toujours ignorés dans le Mapper** (toEntity / updateEntity) : `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deleted`, `deletedBy`.

**Override** : les fichiers générés sont des classes concrètes Spring. Pour surcharger un comportement, étendre la classe générée et annoter avec `@Primary` (service) ou créer une sous-classe du contrôleur.

---

## 4. Starter — Auto-configuration

**Classe** : `io.atalib.autoconfigure.AtaLibAutoConfiguration`

| Configuration | Condition |
|---|---|
| `@EnableJpaAuditing` | Toujours |
| `@EnableAspectJAutoProxy` | Toujours |
| Bean `CrudSecurityAspect` | Si `spring-security-core` est sur le classpath |

---

## 5. Décision : Starter ou Core ?

### Utiliser `ata-lib-spring-boot-starter` si

- Projet créé from scratch sans configuration JPA/AOP préexistante
- Aucune classe annotée `@EnableJpaAuditing` dans le projet

### Utiliser `ata-lib-core` seul si

- Projet JHipster v8 (possède son propre `DatabaseConfiguration`)
- Projet avec `@EnableJpaAuditing(auditorAwareRef = "...")` déjà déclaré
- Contrôle explicite sur les beans Spring souhaité

### Configuration complémentaire requise avec core seul

```java
@EntityScan(basePackages = { "packages.existants", "com.yourapp.domain" })
@EnableJpaRepositories({ "repositories.existantes", "com.yourapp.repository" })

@Configuration
public class AtaLibConfiguration {
    @Bean
    public CrudSecurityAspect crudSecurityAspect() { return new CrudSecurityAspect(); }
}
```

---

## 6. Limitations connues

### 6.1 Lombok ne suit pas les méta-annotations

`@Getter`, `@Setter`, etc. doivent être placés **directement** sur la classe entité.  
`@AtaEntity` ne peut pas les embarquer de façon transparente.

> **Résolution partielle** : `ata-lib-processor` génère les DTOs, Mapper, Service et Controller avec les annotations Lombok appropriées. Seule l'entité elle-même nécessite encore ces annotations en direct.

### 6.2 MapStruct ne suit pas les méta-annotations

MapStruct détecte uniquement les interfaces annotées directement avec `@Mapper`.  
`@AtaMapper` seul ne déclenche pas la génération d'implémentation.

> **Résolution** : `ata-lib-processor` génère l'interface mapper avec `@Mapper(componentModel = "spring")` directement, que MapStruct traite au round APT suivant.

### 6.3 @Entity doit être direct

Spring Boot's entity scanner utilise `AnnotationTypeFilter(Entity.class, false)` — le paramètre `false` désactive la recherche via méta-annotations.

**`@Entity` doit être présent directement sur la classe, en plus de `@AtaEntity`.**

### 6.4 @Builder sur les entités JPA

`@Builder` Lombok ne génère pas de builder pour les champs hérités. MapStruct tente d'y accéder → erreur de compilation.

**Solution** : utiliser `@Getter @Setter @NoArgsConstructor` uniquement sur les entités. Si un builder est nécessaire, utiliser `@SuperBuilder` (requiert aussi `@SuperBuilder` sur `AbstractAuditingEntity`).

---

## 7. Évolutions prévues

| Évolution | Statut | Description |
|---|---|---|
| `ata-lib-processor` | ✅ **Réalisé** | Génération compile-time de tous les fichiers companion via `@AtaEntity` |
| `@SuperBuilder` sur AbstractAuditingEntity | En attente | Permet d'utiliser `@SuperBuilder` sur les entités filles |
| Support `UUID` comme ID | En attente | Paramétrer le type d'ID dans `AbstractAuditingEntity` |
| `BaseFilter` générique | En attente | Classe de filtre avec `Specification<E>` |
| Support multi-tenant | En attente | Champ `tenantId` + filtre automatique |
| Injection `extends` via AST (Phase 3) | En attente | Supprimer le `extends AbstractAuditingEntity` manuel (manipulation AST style Lombok) |
