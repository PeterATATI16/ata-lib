package io.atalib.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;

/**
 * Aspect AOP qui intercepte les méthodes de {@link io.atalib.controller.AbstractGenericController}
 * et vérifie les rôles / permissions déclarés dans {@link SecuredCrud}.
 *
 * <p>Auto-configuré par {@code AtaLibAutoConfiguration} si Spring Security est présent.
 */
@Aspect
public class CrudSecurityAspect {

    @Before("execution(* io.atalib.controller.AbstractGenericController.create(..)) && @within(securedCrud)")
    public void checkCreate(JoinPoint jp, SecuredCrud securedCrud) {
        checkAccess(securedCrud.create(), securedCrud.createPermissions());
    }

    @Before("execution(* io.atalib.controller.AbstractGenericController.update(..)) && @within(securedCrud)")
    public void checkUpdate(JoinPoint jp, SecuredCrud securedCrud) {
        checkAccess(securedCrud.update(), securedCrud.updatePermissions());
    }

    @Before("execution(* io.atalib.controller.AbstractGenericController.getById(..)) && @within(securedCrud)")
    public void checkRead(JoinPoint jp, SecuredCrud securedCrud) {
        checkAccess(securedCrud.read(), securedCrud.readPermissions());
    }

    @Before("execution(* io.atalib.controller.AbstractGenericController.getAll(..)) && @within(securedCrud)")
    public void checkList(JoinPoint jp, SecuredCrud securedCrud) {
        checkAccess(securedCrud.list(), securedCrud.listPermissions());
    }

    @Before("execution(* io.atalib.controller.AbstractGenericController.delete(..)) && @within(securedCrud)")
    public void checkDelete(JoinPoint jp, SecuredCrud securedCrud) {
        checkAccess(securedCrud.delete(), securedCrud.deletePermissions());
    }

    // -------------------------------------------------------------------------

    private void checkAccess(String[] requiredRoles, String[] requiredPermissions) {
        if (requiredRoles.length == 0 && requiredPermissions.length == 0) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }

        boolean hasRole = Arrays.stream(requiredRoles)
                .anyMatch(role -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + role)));

        boolean hasPermission = Arrays.stream(requiredPermissions)
                .anyMatch(perm -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(perm)));

        if (!hasRole && !hasPermission) {
            throw new AccessDeniedException("Insufficient permissions");
        }
    }
}
