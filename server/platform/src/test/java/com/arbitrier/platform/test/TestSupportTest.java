package com.arbitrier.platform.test;

import com.arbitrier.platform.error.PlatformProblemCode;
import com.arbitrier.platform.result.Result;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TestSupportTest {

    @Test
    void test_ids_produce_non_null_ids() {
        assertThat(TestIds.correlationId()).isNotNull();
        assertThat(TestIds.causationId()).isNotNull();
        assertThat(TestIds.messageId()).isNotNull();
        assertThat(TestIds.requestId()).isNotNull();
        assertThat(TestIds.idempotencyKey()).isNotNull();
    }

    @Test
    void fixed_clock_defaults_returns_test_instant() {
        assertThat(FixedClock.defaults().now()).isEqualTo(FixedClock.TEST_INSTANT);
    }

    @Test
    void fixed_clock_at_returns_specified_instant() {
        Instant custom = Instant.parse("2026-06-01T12:00:00Z");
        assertThat(FixedClock.at(custom).now()).isEqualTo(custom);
    }

    @Test
    void platform_assertions_pass_for_success() {
        PlatformAssertions.assertSuccess(Result.success("ok"));
    }

    @Test
    void platform_assertions_pass_for_failure() {
        PlatformAssertions.assertFailure(Result.failure(PlatformProblemCode.INVALID_STATE, "err"));
    }

    @Test
    void platform_assertions_pass_for_failure_code() {
        Result<String> result = Result.failure(PlatformProblemCode.NULL_ARGUMENT, "null");
        PlatformAssertions.assertFailureCode(result, PlatformProblemCode.NULL_ARGUMENT);
    }

    @Test
    void platform_assertions_fail_when_success_expected_but_failure_given() {
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() ->
                        PlatformAssertions.assertSuccess(
                                Result.failure(PlatformProblemCode.INVALID_STATE, "err")));
    }

    @Test
    void platform_assertions_fail_when_wrong_code_given() {
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() ->
                        PlatformAssertions.assertFailureCode(
                                Result.failure(PlatformProblemCode.INVALID_STATE, "err"),
                                PlatformProblemCode.NULL_ARGUMENT));
    }
}
