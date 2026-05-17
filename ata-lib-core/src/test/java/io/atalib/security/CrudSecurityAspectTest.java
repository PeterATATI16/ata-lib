package io.atalib.security;

import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrudSecurityAspectTest {

    private CrudSecurityAspect aspect;
    private JoinPoint jp;

    @BeforeEach
    void setup() {
        aspect = new CrudSecurityAspect();
        jp = mock(JoinPoint.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private SecuredCrud secured(String[] create, String[] createPerms,
                                String[] update, String[] updatePerms,
                                String[] read,   String[] readPerms,
                                String[] list,   String[] listPerms,
                                String[] delete, String[] deletePerms) {
        SecuredCrud s = mock(SecuredCrud.class);
        when(s.create()).thenReturn(create);
        when(s.createPermissions()).thenReturn(createPerms);
        when(s.update()).thenReturn(update);
        when(s.updatePermissions()).thenReturn(updatePerms);
        when(s.read()).thenReturn(read);
        when(s.readPermissions()).thenReturn(readPerms);
        when(s.list()).thenReturn(list);
        when(s.listPermissions()).thenReturn(listPerms);
        when(s.delete()).thenReturn(delete);
        when(s.deletePermissions()).thenReturn(deletePerms);
        return s;
    }

    private SecuredCrud noRestrictions() {
        return secured(new String[]{}, new String[]{}, new String[]{}, new String[]{},
                       new String[]{}, new String[]{}, new String[]{}, new String[]{},
                       new String[]{}, new String[]{});
    }

    private void authenticateAs(String... roles) {
        var authorities = List.of(roles).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null, authorities));
    }

    // -------------------------------------------------------------------------
    // No restrictions
    // -------------------------------------------------------------------------

    @Test
    void allowsAll_whenNoRolesOrPermissionsConfigured() {
        SecuredCrud s = noRestrictions();
        assertThatCode(() -> aspect.checkCreate(jp, s)).doesNotThrowAnyException();
        assertThatCode(() -> aspect.checkUpdate(jp, s)).doesNotThrowAnyException();
        assertThatCode(() -> aspect.checkRead(jp, s)).doesNotThrowAnyException();
        assertThatCode(() -> aspect.checkList(jp, s)).doesNotThrowAnyException();
        assertThatCode(() -> aspect.checkDelete(jp, s)).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Authentication required
    // -------------------------------------------------------------------------

    @Test
    void throwsAccessDenied_whenNotAuthenticated() {
        SecuredCrud s = mock(SecuredCrud.class);
        when(s.create()).thenReturn(new String[]{"ADMIN"});
        when(s.createPermissions()).thenReturn(new String[]{});

        assertThatThrownBy(() -> aspect.checkCreate(jp, s))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }

    // -------------------------------------------------------------------------
    // Role matching
    // -------------------------------------------------------------------------

    @Test
    void allowsAccess_whenUserHasRequiredRole() {
        authenticateAs("ROLE_ADMIN");
        SecuredCrud s = mock(SecuredCrud.class);
        when(s.create()).thenReturn(new String[]{"ADMIN"});
        when(s.createPermissions()).thenReturn(new String[]{});

        assertThatCode(() -> aspect.checkCreate(jp, s)).doesNotThrowAnyException();
    }

    @Test
    void throwsAccessDenied_whenUserLacksRequiredRole() {
        authenticateAs("ROLE_USER");
        SecuredCrud s = mock(SecuredCrud.class);
        when(s.delete()).thenReturn(new String[]{"ADMIN"});
        when(s.deletePermissions()).thenReturn(new String[]{});

        assertThatThrownBy(() -> aspect.checkDelete(jp, s))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Insufficient permissions");
    }

    @Test
    void allowsAccess_whenUserHasOneOfMultipleRoles() {
        authenticateAs("ROLE_MANAGER");
        SecuredCrud s = mock(SecuredCrud.class);
        when(s.update()).thenReturn(new String[]{"ADMIN", "MANAGER"});
        when(s.updatePermissions()).thenReturn(new String[]{});

        assertThatCode(() -> aspect.checkUpdate(jp, s)).doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Permission matching
    // -------------------------------------------------------------------------

    @Test
    void allowsAccess_whenUserHasRequiredPermission() {
        authenticateAs("PERM_READ_STAFF");
        SecuredCrud s = mock(SecuredCrud.class);
        when(s.read()).thenReturn(new String[]{});
        when(s.readPermissions()).thenReturn(new String[]{"PERM_READ_STAFF"});

        assertThatCode(() -> aspect.checkRead(jp, s)).doesNotThrowAnyException();
    }

    @Test
    void allowsAccess_whenUserHasPermissionButNotRole() {
        authenticateAs("PERM_LIST_STAFF");
        SecuredCrud s = mock(SecuredCrud.class);
        when(s.list()).thenReturn(new String[]{"ADMIN"});
        when(s.listPermissions()).thenReturn(new String[]{"PERM_LIST_STAFF"});

        // Roles and permissions are OR'd — permission match is enough
        assertThatCode(() -> aspect.checkList(jp, s)).doesNotThrowAnyException();
    }
}
