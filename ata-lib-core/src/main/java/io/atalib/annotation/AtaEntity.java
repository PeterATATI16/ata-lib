package io.atalib.annotation;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.lang.annotation.*;

/**
 * Composé de {@code @Entity} + {@code @EntityListeners(AuditingEntityListener.class)}.
 *
 * <p>Utilisation :
 * <pre>
 * {@code @AtaEntity}
 * {@code @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor}
 * public class Article extends AbstractAuditingEntity { ... }
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
}
