package com.arbitrier.order.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(OrderServiceTestConfiguration.class)
class OrderServiceApplicationIT {

    @Test
    void contextLoads() {
    }
}
