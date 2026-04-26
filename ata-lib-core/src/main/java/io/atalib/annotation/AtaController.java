package io.atalib.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.*;

/**
 * Composé de {@code @RestController} + {@code @RequestMapping(path)}.
 *
 * <p>Utilisation :
 * <pre>
 * {@code @AtaController("/api/v1/articles")}
 * public class ArticleController
 *         extends AbstractGenericController<...> { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RestController
@RequestMapping
public @interface AtaController {

    @AliasFor(annotation = RequestMapping.class, attribute = "value")
    String[] value() default {};

    @AliasFor(annotation = RequestMapping.class, attribute = "path")
    String[] path() default {};
}
