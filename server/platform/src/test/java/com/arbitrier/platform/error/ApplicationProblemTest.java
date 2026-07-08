package com.arbitrier.platform.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ApplicationProblemTest {

    @Test
    void carries_code_and_message() {
        ApplicationProblemException problem =
                new ApplicationProblemException(PlatformProblemCode.NULL_ARGUMENT, "field was null");

        assertThat(problem.code()).isEqualTo(PlatformProblemCode.NULL_ARGUMENT);
        assertThat(problem.getMessage()).isEqualTo("field was null");
    }

    @Test
    void carries_cause() {
        RuntimeException cause = new RuntimeException("original");
        ApplicationProblemException problem =
                new ApplicationProblemException(PlatformProblemCode.INVALID_STATE, "bad state", cause);

        assertThat(problem.getCause()).isSameAs(cause);
    }

    @Test
    void rejects_null_code() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ApplicationProblemException(null, "msg"));
    }

    @Test
    void platform_problem_codes_have_non_blank_codes_and_descriptions() {
        for (PlatformProblemCode code : PlatformProblemCode.values()) {
            assertThat(code.code()).isNotBlank();
            assertThat(code.description()).isNotBlank();
        }
    }
}
