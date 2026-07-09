package com.arbitrier.credit.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(CreditServiceTestConfiguration.class)
class CreditServiceApplicationIT {

    @Test
    void contextLoads() {
    }
}
