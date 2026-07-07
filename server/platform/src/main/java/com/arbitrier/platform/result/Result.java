package com.arbitrier.platform.result;

import com.arbitrier.platform.error.ApplicationProblem;
import com.arbitrier.platform.error.ProblemCode;

import java.util.Objects;
import java.util.function.Function;

/**
 * Represents the outcome of an operation as either a {@link Success} or a {@link Failure}.
 *
 * <p>Use {@code Result} to express expected failure paths in application use cases without
 * throwing exceptions. Throw {@link ApplicationProblem} only for programming-contract
 * violations or truly unexpected failures.
 *
 * <p>Example:
 * <pre>{@code
 * Result<Order> result = placeOrderUseCase.execute(command);
 * return switch (result) {
 *     case Result.Success<Order> s -> ResponseEntity.ok(s.value());
 *     case Result.Failure<Order> f -> ResponseEntity.unprocessableEntity()
 *             .body(f.message());
 * };
 * }</pre>
 *
 * @param <T> the type of the success value
 */
public sealed interface Result<T> permits Result.Success, Result.Failure {

    /**
     * Successful outcome carrying a non-null value.
     *
     * @param <T> the success value type
     */
    record Success<T>(T value) implements Result<T> {

        /** Validates that value is not null. */
        public Success {
            Objects.requireNonNull(value, "Success.value must not be null");
        }
    }

    /**
     * Failure outcome carrying a typed code and a human-readable message.
     *
     * @param <T> phantom type matching the enclosing {@link Result}
     */
    record Failure<T>(ProblemCode code, String message) implements Result<T> {

        /** Validates that code and message are not null. */
        public Failure {
            Objects.requireNonNull(code, "Failure.code must not be null");
            Objects.requireNonNull(message, "Failure.message must not be null");
        }
    }

    // ── Factory methods ───────────────────────────────────────────────────────

    /**
     * Creates a successful result.
     *
     * @param value the success value; must not be null
     * @param <T>   the value type
     * @return a {@link Success} wrapping {@code value}
     */
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a failure result.
     *
     * @param code    the typed problem code; must not be null
     * @param message human-readable description; must not be null
     * @param <T>     phantom type
     * @return a {@link Failure}
     */
    static <T> Result<T> failure(ProblemCode code, String message) {
        return new Failure<>(code, message);
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    /** Returns {@code true} if this is a {@link Success}. */
    default boolean isSuccess() {
        return this instanceof Success<?>;
    }

    /** Returns {@code true} if this is a {@link Failure}. */
    default boolean isFailure() {
        return this instanceof Failure<?>;
    }

    // ── Extraction ────────────────────────────────────────────────────────────

    /**
     * Returns the success value or throws {@link ApplicationProblem} if this is a failure.
     *
     * @return the success value
     * @throws ApplicationProblem if this is a {@link Failure}
     */
    @SuppressWarnings("unchecked")
    default T valueOrThrow() {
        return switch (this) {
            case Success<?> s -> (T) s.value();
            case Failure<?> f -> throw new ApplicationProblem(f.code(), f.message());
        };
    }

    /**
     * Transforms the success value, leaving failures unchanged.
     *
     * @param mapper function applied to the success value; must not be null
     * @param <U>    the mapped value type
     * @return a new {@code Result} with the mapped value, or this failure unchanged
     */
    @SuppressWarnings("unchecked")
    default <U> Result<U> map(Function<T, U> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return switch (this) {
            case Success<?> s -> Result.success(mapper.apply((T) s.value()));
            case Failure<?> f -> Result.failure(f.code(), f.message());
        };
    }
}
