package com.arbitrier.platform.error;

import java.util.Objects;

/**
 * Runtime exception carrying a typed {@link ProblemCode}.
 *
 * <p>Thrown when a {@link com.arbitrier.platform.result.Result} failure is unwrapped via
 * {@code valueOrThrow()}, or when an application service detects an unrecoverable error.
 *
 * <p>This is an unchecked exception — callers choose whether to catch it.
 * Use {@link com.arbitrier.platform.result.Result} for expected failure paths.
 */
public final class ApplicationProblem extends RuntimeException {

    private final ProblemCode code;

    /**
     * Creates an {@code ApplicationProblem} with a typed code and descriptive message.
     *
     * @param code    the typed problem code; must not be null
     * @param message human-readable description; must not be null
     */
    public ApplicationProblem(ProblemCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    /**
     * Creates an {@code ApplicationProblem} wrapping an upstream cause.
     *
     * @param code    the typed problem code; must not be null
     * @param message human-readable description; must not be null
     * @param cause   the upstream exception
     */
    public ApplicationProblem(ProblemCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code must not be null");
    }

    /** Returns the typed problem code. */
    public ProblemCode code() {
        return code;
    }
}
