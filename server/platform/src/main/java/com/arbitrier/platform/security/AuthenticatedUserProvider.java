package com.arbitrier.platform.security;

import java.util.Optional;

/**
 * Resolves the currently authenticated user from whatever security context is active.
 *
 * <p>Implement this interface in the adapter layer (e.g. using Spring Security's
 * {@code SecurityContextHolder}) and inject it into application services or components
 * that need the current user without accepting it as a method argument.
 *
 * <p>For REST controllers, prefer accepting the authenticated user as a method parameter
 * (mapped from the framework's {@code Authentication} object) rather than calling this
 * provider inside the controller body.
 *
 * <p>Layer: security
 * <p>Module: platform
 */
public interface AuthenticatedUserProvider {

    /**
     * Returns the currently authenticated user, or empty if no authentication is active.
     *
     * @return the current authenticated user, or {@link Optional#empty()} for anonymous/unauthenticated context
     */
    Optional<AuthenticatedUser> currentUser();
}
