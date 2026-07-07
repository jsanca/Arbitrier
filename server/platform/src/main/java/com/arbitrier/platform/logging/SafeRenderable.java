package com.arbitrier.platform.logging;

/**
 * Implemented by types that can produce a PII-safe string for display in UIs or reports.
 *
 * <p>Similar to {@link SafeLoggable} but intended for user-facing surfaces rather than
 * structured log output. The rendered form may be slightly more descriptive than a log string
 * (e.g. masked email shown to an operator dashboard) while still omitting raw PII.
 */
public interface SafeRenderable {

    /**
     * Returns a representation safe for display in operator UIs or exported reports.
     *
     * <p>Must never expose raw PII. Partial masking is acceptable when it aids operator
     * identification (e.g. {@code "****1234"} for a card number).
     *
     * @return a non-null, PII-safe display string
     */
    String renderSafe();
}
