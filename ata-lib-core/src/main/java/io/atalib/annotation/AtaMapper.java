package io.atalib.annotation;

import org.mapstruct.Mapper;

import java.lang.annotation.*;

/**
 * Raccourci pour {@code @Mapper(componentModel = "spring")}.
 *
 * <p><strong>Important :</strong> MapStruct ne détecte pas {@code @Mapper} via méta-annotation.
 * Cette annotation DOIT remplacer {@code @Mapper} directement sur l'interface — elle ne peut
 * pas être utilisée en supplément. Si MapStruct ne génère pas l'implémentation, repassez à
 * {@code @Mapper(componentModel = "spring")} directement.
 *
 * <p>Utilisation :
 * <pre>
 * {@code @AtaMapper}
 * public interface ArticleMapper { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Documented
@Mapper(componentModel = "spring")
public @interface AtaMapper {
}
