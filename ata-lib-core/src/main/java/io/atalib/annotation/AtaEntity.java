package io.atalib.annotation;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.lang.annotation.*;

/**
 * Composé de {@code @Entity} + {@code @EntityListeners(AuditingEntityListener.class)}.
 *
 * <p>Déclenche également {@code ata-lib-processor} qui génère automatiquement :
 * ResponseDto, RequestDto, Mapper (MapStruct), Repository, ServiceImpl et Controller.
 *
 * <p>Utilisation :
 * <pre>
 * {@code @AtaEntity(table = "staff", responseExclude = {"password"})}
 * {@code @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor}
 * public class Staff extends AbstractAuditingEntity { ... }
 * </pre>
 *
 * <p>Note : Lombok ne supporte pas les méta-annotations — {@code @Getter}, {@code @Setter},
 * etc. doivent être placés directement sur la classe.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Entity
@EntityListeners(AuditingEntityListener.class)
public @interface AtaEntity {

    /** Nom de la table SQL. Par défaut : snake_case du nom de la classe. */
    String table() default "";

    /** Champs à exclure du ResponseDto. Les champs d'audit sont toujours exclus. */
    String[] responseExclude() default {};

    /** Champs à exclure du RequestDto. L'id et les champs d'audit sont toujours exclus. */
    String[] requestExclude() default {};

    /** Base URL du contrôleur REST généré. Par défaut : /{nomEntitéEnMinuscules}. */
    String baseUrl() default "";

    /** Type de l'identifiant. Par défaut : Long. Exemples : UUID.class, Integer.class. */
    Class<?> idType() default Long.class;
}
