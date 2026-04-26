package io.atalib.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditUtils {

    private AuditUtils() {
    }

    public static String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return "SYSTEM";
            }
            return auth.getName();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }
}
