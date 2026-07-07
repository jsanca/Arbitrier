package com.arbitrier.platform.error;

/**
 * Platform-level problem codes for programming-contract violations.
 *
 * <p>These codes represent bugs or misuse (null arguments, blank values, invalid state),
 * not business failures. Business failures use domain-specific {@link ProblemCode} enums.
 */
public enum PlatformProblemCode implements ProblemCode {

    NULL_ARGUMENT("PLATFORM_NULL_ARGUMENT", "A required argument was null"),
    BLANK_ARGUMENT("PLATFORM_BLANK_ARGUMENT", "A required string argument was blank or empty"),
    INVALID_STATE("PLATFORM_INVALID_STATE", "The system reached an invalid state");

    private final String code;
    private final String description;

    PlatformProblemCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String description() {
        return description;
    }
}
