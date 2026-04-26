package io.atalib.autoconfigure;

import io.atalib.security.CrudSecurityAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Auto-configuration ata-lib.
 *
 * <p>Active automatiquement :
 * <ul>
 *   <li>JPA Auditing ({@code @EnableJpaAuditing}) — alimente createdAt, updatedAt sur BaseEntity</li>
 *   <li>AspectJ Proxy ({@code @EnableAspectJAutoProxy}) — nécessaire pour CrudSecurityAspect</li>
 *   <li>{@link CrudSecurityAspect} — uniquement si Spring Security est présent sur le classpath</li>
 * </ul>
 */
@AutoConfiguration
@EnableJpaAuditing
@EnableAspectJAutoProxy
public class AtaLibAutoConfiguration {

    /**
     * Enregistre l'aspect de sécurité CRUD uniquement si Spring Security est présent.
     * Aucune action requise côté projet — suffit d'avoir spring-boot-starter-security dans les dépendances.
     */
    // Activé uniquement si Spring Security est présent sur le classpath du projet cible
    @Bean
    @ConditionalOnClass(name = "org.springframework.security.core.Authentication")
    @ConditionalOnMissingBean(CrudSecurityAspect.class)
    public CrudSecurityAspect crudSecurityAspect() {
        return new CrudSecurityAspect();
    }
}
