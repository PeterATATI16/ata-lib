package io.atalib.annotation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Composé de {@code @Service} + {@code @Transactional}.
 *
 * <p>Utilisation :
 * <pre>
 * {@code @AtaService}
 * public class ArticleServiceImpl
 *         extends AbstractGenericService<...>
 *         implements ArticleService { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
@Transactional
public @interface AtaService {
}
