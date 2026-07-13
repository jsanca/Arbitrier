package com.arbitrier.credit.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
        "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
        "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"
})
@Import(CreditServiceTestConfiguration.class)
class CreditServiceApplicationIT {

    @Test
    void contextLoads() {
    }
}
