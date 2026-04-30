# ata-lib — Spécification Technique et Fonctionnelle

**Version** : 1.2.0  
**Stack** : Java 17 · Spring Boot 3.3+ · Spring Data JPA · MapStruct · Lombok  
**Dépôt** : https://github.com/PeterATATI16/ata-lib

---

## 1. Objectif

ata-lib est une librairie Java extractant les patterns CRUD génériques communs à tous les microservices Spring Boot. Elle élimine le boilerplate répété en fournissant :

- Des classes abstraites et annotations composées réutilisables (`ata-lib-core`)
- Une auto-configuration Spring Boot optionnelle (`ata-lib-spring-boot-starter`)
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
| spring-boot-starter-data-jpa | compile | AbstractAuditingBase, JpaRepository |
| spring-boot-starter-web | compile | AbstractGenericController |
| spring-boot-starter-validation | compile | @Valid sur les endpoints |
| spring-boot-starter-aop | compile | CrudSecurityAspect |
| spring-boot-starter-security | **optional** | CrudSecurityAspect (pas chargé si absent) |
| mapstruct | compile | Annotations @Mapper |
| lombok | **optional** | Classes de domaine uniquement |

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

### 3.1 Hiérarchie de domaine

**Package** : `io.atalib.domain`

```
AbstractAuditingBase         @MappedSuperclass — audit fields + lifecycle hooks
    ├── AbstractAuditingEntity        @MappedSuperclass — @Id Long id, GenerationType.IDENTITY
    └── AbstractUuidAuditingEntity    @MappedSuperclass — @Id UUID id, GenerationType.UUID
```

#### AbstractAuditingBase

Classe de base commune. Ne déclare pas de clé primaire.

**Champs**

| Champ | Type | Colonne | Description |
|---|---|---|---|
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

#### AbstractAuditingEntity

Étend `AbstractAuditingBase`. Ajoute `@Id Long id` avec `GenerationType.IDENTITY`.  
À utiliser pour les entités avec clé primaire auto-incrémentée.

#### AbstractUuidAuditingEntity

Étend `AbstractAuditingBase`. Ajoute `@Id UUID id` avec `GenerationType.UUID`.  
À utiliser pour les entités avec clé primaire UUID générée par la base.  
Requiert PostgreSQL 9.4+ ou toute base supportant `gen_random_uuid()`.

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
| `delete` | `void delete(ID id)` | Suppression (soft si extends AbstractAuditingBase) |
| `getAllWithoutPagination` | `List<RES> getAllWithoutPagination()` | Liste complète — implémentée dans le ServiceImpl généré |

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

**Comportement de `delete()`** : si l'entité est une `AbstractAuditingBase`, appelle `softDelete()` puis sauvegarde. Sinon, suppression physique.

**`fetchEntities(Pageable)`** : retourne toutes les entités triées par `updatedAt DESC`. Override pour filtrer.

**Flux des hooks lifecycle**

```
POST   → beforeCreate → [map DTO → entity] → afterMapping → [save] → afterCreate
PUT    → beforeUpdate → [charger entity] → [map DTO → entity] → afterUpdateMapping → [save] → afterUpdate
DELETE → beforeDelete → [softDelete] → afterDelete
```

**Hooks disponibles**

| Hook | Paramètres | Quand |
|---|---|---|
| `beforeCreate` | `REQUEST_DTO dto` | Avant le mapping DTO → entity |
| `afterMapping` | `ENTITY entity, REQUEST_DTO dto` | Après le mapping, avant le save |
| `afterCreate` | `ENTITY entity, REQUEST_DTO dto` | Après le save |
| `beforeUpdate` | `ID id, REQUEST_DTO dto` | Avant le chargement de l'entité |
| `afterUpdateMapping` | `ENTITY entity, REQUEST_DTO dto` | Après le mapping partiel, avant le save |
| `afterUpdate` | `ENTITY entity, REQUEST_DTO dto` | Après le save |
| `beforeDelete` | `ID id` | Avant le soft delete |
| `afterDelete` | `ID id` | Après le soft delete |

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

Intercepte les méthodes de `AbstractGenericController` via `@Before` pointcuts. Lit `@SecuredCrud` et vérifie les droits via `SecurityContextHolder`. Lève `AccessDeniedException` si les contraintes ne sont pas satisfaites.

**Activation** : automatique via starter (si Spring Security sur le classpath) ou bean manuel via `@Configuration`.

---

### 3.7 Annotations composées

**Package** : `io.atalib.annotation`

| Annotation | Compose |
|---|---|
| `@AtaEntity(responseExclude, requestExclude, baseUrl)` | `@Entity + @EntityListeners(AuditingEntityListener.class)` + déclenche le processor |
| `@AtaService` | `@Service + @Transactional` |
| `@AtaController(value/path)` | `@RestController + @RequestMapping` |

**Attributs de @AtaEntity**

| Attribut | Type | Défaut | Description |
|---|---|---|---|
| `responseExclude` | `String[]` | `{}` | Champs à exclure du ResponseDto (champs d'audit toujours exclus) |
| `requestExclude` | `String[]` | `{}` | Champs à exclure du RequestDto (id + champs d'audit toujours exclus) |
| `baseUrl` | `String` | `/{nomEntitéMinuscules}` | Base URL du contrôleur généré |

**Note** : le nom de la table SQL est contrôlé par `@jakarta.persistence.Table(name = "...")`, pas par ata-lib. Obligatoire si le nom de la classe est un mot réservé SQL (`user`, `order`, `group`...).

---

### 3.8 AtaEntityProcessor

**Package** : `io.atalib.processor`  
**Type** : `AbstractProcessor` (APT standard Java)  
**Déclenché par** : `@AtaEntity` sur une classe

Génère 6 fichiers source dans le même package que l'entité, lors de la compilation (round 1 de l'APT). MapStruct traite l'interface mapper générée au round suivant.

**Fichiers générés**

| Fichier | Contenu |
|---|---|
| `{Entity}ResponseDto` | Champs de l'entité + `id`, hors `responseExclude` et champs d'audit |
| `{Entity}RequestDto` | Champs de l'entité, hors `requestExclude`, `id` et champs d'audit |
| `{Entity}Mapper` | Interface `@Mapper(componentModel = "spring")` avec `toDto`, `toEntity`, `updateEntity` |
| `{Entity}Repository` | `JpaRepository<Entity, ID>` + `findAllByDeletedFalse(Pageable)` + `findAllByDeletedFalse()` |
| `{Entity}ServiceImpl` | Extends `AbstractGenericService`, `@AtaService`, override de `fetchEntities` et `getAllWithoutPagination` |
| `{Entity}Controller` | Extends `AbstractGenericController`, `@AtaController(baseUrl)`, overrides explicites pour `getById`, `update`, `delete` |

**Détection du type d'ID** : le processeur parcourt la hiérarchie de la classe via `processingEnv.getTypeUtils()` pour trouver le champ annoté `@jakarta.persistence.Id` et lire son type. Retourne `Long` par défaut si aucun champ `@Id` n'est trouvé.

**DTOs avec getters/setters explicites** : les DTOs générés n'utilisent pas Lombok. Getters, setters, constructeur sans arguments et constructeur complet sont générés directement dans le source, pour garantir que MapStruct les voit dès le premier round APT.

**Mirroring des annotations** : les annotations de validation (`@NotBlank`, `@Email`, `@Size`, etc.) présentes sur les champs de l'entité sont automatiquement copiées sur les DTOs générés. Les annotations JPA (`jakarta.persistence.*`), Lombok et Spring Data sont exclues.

**Champs d'audit ignorés dans le Mapper** : `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deleted`, `deletedBy`.

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

---

## 6. Limitations connues

### 6.1 `@Entity` doit être direct

Spring Boot's entity scanner utilise `AnnotationTypeFilter(Entity.class, false)` — le paramètre `false` désactive la recherche via méta-annotations. `@Entity` doit être présent directement sur la classe entité, en plus de `@AtaEntity`.

### 6.2 Lombok ne suit pas les méta-annotations

`@Getter`, `@Setter`, etc. doivent être placés directement sur la classe entité.

### 6.3 `@Builder` conflicte avec les champs hérités

Lombok `@Builder` ne génère pas de builder pour les champs hérités. Utiliser `@Getter @Setter @NoArgsConstructor` sur les entités. Si un builder est requis, utiliser `@SuperBuilder` (nécessite `@SuperBuilder` sur la classe de base également).

### 6.4 Mots réservés SQL

`user`, `order`, `group` et d'autres mots réservés PostgreSQL nécessitent `@Table(name = "users")` sur la classe entité. Sans ça, Hibernate génère un DDL invalide au démarrage.

---

## 7. Évolutions prévues

| Évolution | Statut | Description |
|---|---|---|
| `ata-lib-processor` — génération compile-time | ✅ Réalisé | Génération de 6 fichiers via `@AtaEntity` |
| Support UUID comme type d'ID | ✅ Réalisé | `AbstractUuidAuditingEntity` + détection automatique du type `@Id` |
| Getters/setters explicites dans les DTOs | ✅ Réalisé | Résout le conflit d'ordre APT Lombok/MapStruct |
| Overrides explicites dans le Controller généré | ✅ Réalisé | Résout la résolution de type générique dans SpringDoc/Swagger |
| Spring Security optionnelle | ✅ Réalisé | `AuditUtils` détecte la présence sur le classpath via `Class.forName` |
| `BaseFilter` générique | En attente | Classe de filtre avec `Specification<E>` |
| Support multi-tenant | En attente | Champ `tenantId` + filtre automatique |
| Suppression du `extends AbstractAuditingEntity` manuel (Phase 3) | En attente | Manipulation AST style Lombok |
