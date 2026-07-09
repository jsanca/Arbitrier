package com.arbitrier.platform.security;

import com.arbitrier.platform.validation.Require;

import java.util.Set;

/**
 * Immutable representation of an authenticated principal, decoupled from any security framework.
 *
 * <p>Constructed by inbound adapters (e.g. REST controller) from the authentication context
 * (e.g. a JWT) before the request enters the application layer. The application layer
 * receives only this platform type — never a Spring Security or Keycloak-specific object.
 *
 * <p>The {@code userId} maps to the JWT {@code sub} (subject) claim, which is the canonical
 * user identity for Arbitrier (see ARB-010).
 *
 * <p>Layer: security
 * <p>Module: platform
 */
public record AuthenticatedUser(String userId, Set<String> authorities) {

    /** Validates that userId is present and authorities is not null. */
    public AuthenticatedUser {
        Require.notBlank(userId, "AuthenticatedUser.userId");
        Require.notNull(authorities, "AuthenticatedUser.authorities");
        authorities = Set.copyOf(authorities);
    }

    /**
     * Creates an {@code AuthenticatedUser} with no granted authorities.
     *
     * @param userId the subject claim from the JWT; must not be blank
     * @return an authenticated user with an empty authority set
     */
    public static AuthenticatedUser ofUserId(String userId) {
        return new AuthenticatedUser(userId, Set.of());
    }

    /** Returns {@code true} if this user holds the given authority. */
    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

    /** Safe string representation — logs userId but not the full authority set to avoid verbose output. */
    @Override
    public String toString() {
        return "AuthenticatedUser{userId='" + userId + "'}";
    }
}
