package com.arbitrier.platform.test;

import com.arbitrier.platform.error.ProblemCode;
import com.arbitrier.platform.result.Result;

/**
 * Assertion helpers for platform primitives used in unit and integration tests.
 *
 * <p>Throws {@link AssertionError} on failure so these can be used with any test framework
 * without pulling in framework-specific assertion dependencies.
 *
 * <p>Example:
 * <pre>{@code
 * Result<Order> result = useCase.execute(command);
 * PlatformAssertions.assertSuccess(result);
 * }</pre>
 */
public final class PlatformAssertions {

    private PlatformAssertions() {
    }

    /**
     * Asserts that {@code result} is a {@link Result.Success}.
     *
     * @param result the result to check; must not be null
     * @throws AssertionError if {@code result} is a {@link Result.Failure}
     */
    public static void assertSuccess(Result<?> result) {
        if (!result.isSuccess()) {
            Result.Failure<?> f = (Result.Failure<?>) result;
            throw new AssertionError(
                    "Expected Result.Success but got Failure[code=" + f.code().code()
                    + ", message=" + f.message() + "]");
        }
    }

    /**
     * Asserts that {@code result} is a {@link Result.Failure}.
     *
     * @param result the result to check; must not be null
     * @throws AssertionError if {@code result} is a {@link Result.Success}
     */
    public static void assertFailure(Result<?> result) {
        if (!result.isFailure()) {
            throw new AssertionError("Expected Result.Failure but got Success");
        }
    }

    /**
     * Asserts that {@code result} is a {@link Result.Failure} with the given code.
     *
     * @param result       the result to check; must not be null
     * @param expectedCode the expected problem code; must not be null
     * @throws AssertionError if {@code result} is a Success or has a different code
     */
    public static void assertFailureCode(Result<?> result, ProblemCode expectedCode) {
        assertFailure(result);
        Result.Failure<?> f = (Result.Failure<?>) result;
        if (!expectedCode.code().equals(f.code().code())) {
            throw new AssertionError(
                    "Expected failure code [" + expectedCode.code()
                    + "] but got [" + f.code().code() + "]");
        }
    }
}
