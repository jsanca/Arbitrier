package com.arbitrier.orchestrator.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(OrchestratorServiceTestConfiguration.class)
class OrchestratorServiceApplicationIT {

    @Test
    void contextLoads() {
    }
}
