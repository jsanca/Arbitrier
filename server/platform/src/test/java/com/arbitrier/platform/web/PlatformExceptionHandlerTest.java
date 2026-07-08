package com.arbitrier.platform.web;

import com.arbitrier.platform.error.ApplicationProblemException;
import com.arbitrier.platform.error.PlatformProblemCode;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlatformExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new PlatformExceptionHandler())
                .build();
    }

    @Test
    void application_problem_exception_maps_to_422() throws Exception {
        mockMvc.perform(get("/test/app-problem"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(PlatformProblemCode.INVALID_STATE.code()))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void illegal_argument_exception_maps_to_400() throws Exception {
        mockMvc.perform(get("/test/illegal-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void bean_validation_failure_maps_to_400_with_field_detail() throws Exception {
        mockMvc.perform(post("/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void problem_response_code_matches_platform_problem_code() throws Exception {
        mockMvc.perform(get("/test/app-problem"))
                .andExpect(jsonPath("$.message").value("invalid state for test"));
    }

    // ── test controller ───────────────────────────────────────────────────────

    @RestController
    static class ThrowingController {

        @GetMapping("/test/app-problem")
        void throwAppProblem() {
            throw new ApplicationProblemException(PlatformProblemCode.INVALID_STATE, "invalid state for test");
        }

        @GetMapping("/test/illegal-arg")
        void throwIllegalArg() {
            throw new IllegalArgumentException("field must not be blank");
        }

        @PostMapping("/test/validated")
        void validated(@RequestBody @Valid ValidatedRequest body) {
        }

        record ValidatedRequest(@NotBlank String name) {
        }
    }
}
