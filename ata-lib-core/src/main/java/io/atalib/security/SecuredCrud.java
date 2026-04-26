package io.atalib.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sécurité CRUD déclarative par rôle et/ou permission.
 *
 * <p>Placer sur un contrôleur qui étend {@link io.atalib.controller.AbstractGenericController}.
 * L'aspect {@link CrudSecurityAspect} intercepte chaque opération et vérifie les accès.
 *
 * <p>Exemple :
 * <pre>
 * {@literal @}SecuredCrud(
 *     create            = {"ADMIN"},
 *     createPermissions = {"CREER_ARTICLE"},
 *     update            = {"ADMIN", "MANAGER"},
 *     readPermissions   = {"VOIR_ARTICLE"},
 *     listPermissions   = {"LISTER_ARTICLES"},
 *     delete            = {"ADMIN"}
 * )
 * </pre>
 *
 * <p>Les tableaux vides signifient "aucune restriction pour cette opération".
 * Les rôles sont préfixés automatiquement de {@code ROLE_}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SecuredCrud {

    /** Rôles autorisés à créer (ex: "ADMIN", "MANAGER"). Préfixe ROLE_ ajouté automatiquement. */
    String[] create() default {};

    /** Permissions fines autorisées à créer (ex: "CREER_ARTICLE"). */
    String[] createPermissions() default {};

    /** Rôles autorisés à modifier. */
    String[] update() default {};

    /** Permissions fines autorisées à modifier. */
    String[] updatePermissions() default {};

    /** Rôles autorisés à lire un élément par id. */
    String[] read() default {};

    /** Permissions fines autorisées à lire un élément par id. */
    String[] readPermissions() default {};

    /** Rôles autorisés à supprimer. */
    String[] delete() default {};

    /** Permissions fines autorisées à supprimer. */
    String[] deletePermissions() default {};

    /** Rôles autorisés à lister (getAll). */
    String[] list() default {};

    /** Permissions fines autorisées à lister (getAll). */
    String[] listPermissions() default {};
}
