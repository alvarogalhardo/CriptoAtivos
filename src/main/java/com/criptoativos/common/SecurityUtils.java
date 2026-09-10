package com.criptoativos.common;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * The authenticated user's id, taken from the JWT subject.
     *
     * <p>Endpoints resolve the caller this way rather than from a path variable, so one user can
     * never address another user's wallet or transactions.
     */
    public static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No authenticated JWT principal in the security context.");
        }
        return UUID.fromString(jwt.getSubject());
    }
}
