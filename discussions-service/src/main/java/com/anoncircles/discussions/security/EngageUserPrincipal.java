package com.anoncircles.discussions.security;

import java.security.Principal;
import java.util.UUID;

/**
 * Authenticated-user record placed on {@code SecurityContext} by
 * {@link EngageAuthFilter}. Controllers inject via
 * {@code @AuthenticationPrincipal EngageUserPrincipal user}.
 */
public record EngageUserPrincipal(UUID id, String displayName) implements Principal {

    @Override
    public String getName() {
        return id.toString();
    }
}
