package com.arbitrier.platform.web;

import com.arbitrier.platform.error.ApplicationProblemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler that maps platform exceptions to {@link ProblemResponse}.
 *
 * <p>Handles:
 * <ul>
 *   <li>{@link ApplicationProblemException} → 422 Unprocessable Entity</li>
 *   <li>{@link IllegalArgumentException} → 400 Bad Request</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 Bad Request with field detail</li>
 * </ul>
 *
 * <p>Layer: platform/web
 * <p>Module: platform
 */
@RestControllerAdvice
public class PlatformExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PlatformExceptionHandler.class);

    /**
     * Maps {@link ApplicationProblemException} to an HTTP status derived from
     * {@link com.arbitrier.platform.error.ProblemCode#httpStatus()}.
     *
     * <p>Defaults to 422 Unprocessable Entity for business-rule violations.
     * Authorization failures (403), not-found (404), etc. use their own problem codes.
     */
    @ExceptionHandler(ApplicationProblemException.class)
    public ResponseEntity<ProblemResponse> handleApplicationProblem(ApplicationProblemException ex) {
        int status = ex.code().httpStatus();
        log.warn("Application problem: code={}, httpStatus={}, message={}", ex.code().code(), status, ex.getMessage());
        return ResponseEntity
                .status(status)
                .body(ProblemResponse.of(ex.code().code(), ex.getMessage(), status));
    }

    /** Maps {@link IllegalArgumentException} (e.g. from {@code Require}) to 400. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Validation failure: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(ProblemResponse.of("VALIDATION_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    /** Maps Bean Validation failures ({@code @Valid}) to 400 with field-level detail. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.debug("Bean validation failure: {}", detail);
        return ResponseEntity
                .badRequest()
                .body(ProblemResponse.of("VALIDATION_ERROR", detail, HttpStatus.BAD_REQUEST.value()));
    }
}
