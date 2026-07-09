package com.arbitrier.inventory.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(InventoryServiceTestConfiguration.class)
class InventoryServiceApplicationIT {

    @Test
    void contextLoads() {
    }
}
