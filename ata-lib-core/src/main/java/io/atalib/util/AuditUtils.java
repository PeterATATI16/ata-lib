package io.atalib.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AuditUtils {

    private static final boolean SECURITY_PRESENT;

    static {
        boolean present;
        try {
            Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            present = true;
        } catch (ClassNotFoundException e) {
            present = false;
        }
        SECURITY_PRESENT = present;
    }

    private AuditUtils() {}

    public static String getCurrentUsername() {
        if (!SECURITY_PRESENT) return "SYSTEM";
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return "SYSTEM";
            return auth.getName();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }
}
