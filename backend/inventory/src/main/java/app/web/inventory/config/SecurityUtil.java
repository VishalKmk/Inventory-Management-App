package app.web.inventory.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.UUID;

public class SecurityUtil {
    @SuppressWarnings("unchecked")
    public static UUID getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Cannot get current user ID");
        }
        Map<String, Object> principal = (Map<String, Object>) auth.getPrincipal();
        return (UUID) principal.get("userId");
    }

    public static String getCurrentUserEmail() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null)
            return null;
        Object principal = a.getPrincipal();
        if (principal instanceof Map) {
            return (String) ((Map<?, ?>) principal).get("email");
        }
        return null;
    }
}
