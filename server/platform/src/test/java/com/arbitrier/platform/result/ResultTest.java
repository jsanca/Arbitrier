package com.arbitrier.platform.result;

import com.arbitrier.platform.error.ApplicationProblem;
import com.arbitrier.platform.error.PlatformProblemCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ResultTest {

    @Test
    void success_carries_value() {
        Result<String> result = Result.success("hello");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
    }

    @Test
    void success_value_or_throw_returns_value() {
        String value = Result.<String>success("hello").valueOrThrow();
        assertThat(value).isEqualTo("hello");
    }

    @Test
    void success_rejects_null_value() {
        assertThatNullPointerException().isThrownBy(() -> Result.success(null));
    }

    @Test
    void failure_carries_code_and_message() {
        Result<String> result = Result.failure(PlatformProblemCode.INVALID_STATE, "oops");
        assertThat(result.isFailure()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<?>) result).code()).isEqualTo(PlatformProblemCode.INVALID_STATE);
        assertThat(((Result.Failure<?>) result).message()).isEqualTo("oops");
    }

    @Test
    void failure_value_or_throw_throws_application_problem() {
        Result<String> result = Result.failure(PlatformProblemCode.INVALID_STATE, "bad state");

        assertThatExceptionOfType(ApplicationProblem.class)
                .isThrownBy(result::valueOrThrow)
                .withMessage("bad state");
    }

    @Test
    void failure_rejects_null_code() {
        assertThatNullPointerException().isThrownBy(() -> Result.failure(null, "msg"));
    }

    @Test
    void failure_rejects_null_message() {
        assertThatNullPointerException()
                .isThrownBy(() -> Result.failure(PlatformProblemCode.INVALID_STATE, null));
    }

    @Test
    void map_transforms_success_value() {
        Result<Integer> result = Result.<String>success("hello").map(String::length);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.valueOrThrow()).isEqualTo(5);
    }

    @Test
    void map_passes_through_failure_unchanged() {
        Result<String> original = Result.failure(PlatformProblemCode.INVALID_STATE, "err");
        Result<Integer> mapped = original.map(String::length);

        assertThat(mapped.isFailure()).isTrue();
        assertThat(((Result.Failure<?>) mapped).code()).isEqualTo(PlatformProblemCode.INVALID_STATE);
    }
}
