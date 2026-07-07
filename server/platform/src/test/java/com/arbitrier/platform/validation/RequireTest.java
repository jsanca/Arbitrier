package com.arbitrier.platform.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RequireTest {

    @Test
    void not_null_returns_value_when_present() {
        assertThat(Require.notNull("hello", "field")).isEqualTo("hello");
    }

    @Test
    void not_null_throws_when_null() {
        assertThatNullPointerException()
                .isThrownBy(() -> Require.notNull(null, "myField"))
                .withMessageContaining("myField");
    }

    @Test
    void not_blank_returns_value_when_valid() {
        assertThat(Require.notBlank("hello", "field")).isEqualTo("hello");
    }

    @Test
    void not_blank_throws_for_null() {
        assertThatNullPointerException()
                .isThrownBy(() -> Require.notBlank(null, "field"));
    }

    @Test
    void not_blank_throws_for_empty_string() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Require.notBlank("", "field"))
                .withMessageContaining("field");
    }

    @Test
    void not_blank_throws_for_whitespace_only() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Require.notBlank("   ", "field"));
    }

    @Test
    void not_empty_returns_collection_when_non_empty() {
        List<String> list = List.of("a", "b");
        assertThat(Require.notEmpty(list, "items")).isSameAs(list);
    }

    @Test
    void not_empty_throws_for_empty_collection() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Require.notEmpty(List.of(), "items"))
                .withMessageContaining("items");
    }

    @Test
    void is_true_passes_for_true_condition() {
        Require.isTrue(1 == 1, "should not throw");
    }

    @Test
    void is_true_throws_for_false_condition() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Require.isTrue(false, "condition failed"))
                .withMessage("condition failed");
    }
}
