package com.arbitrier.platform.web;

/**
 * Uniform error response body returned by {@link PlatformExceptionHandler}.
 *
 * <p>Serialised to JSON by Spring MVC for all handled exceptions.
 *
 * @param code    typed problem code string; never null
 * @param message human-readable description safe for external consumption
 * @param status  HTTP status code
 *
 * <p>Layer: platform/web
 * <p>Module: platform
 */
public record ProblemResponse(String code, String message, int status) {

    /** Factory method for concise construction. */
    public static ProblemResponse of(String code, String message, int status) {
        return new ProblemResponse(code, message, status);
    }
}
