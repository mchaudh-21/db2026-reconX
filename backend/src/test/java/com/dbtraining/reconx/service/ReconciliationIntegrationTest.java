package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.ReconResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Import(ReconciliationIntegrationTest.TestConfig.class)
class ReconciliationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("reconx")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "spring.datasource.driver-class-name",
                postgres::getDriverClassName
        );

        registry.add(
                "spring.jpa.database-platform",
                () -> "org.hibernate.dialect.PostgreSQLDialect"
        );

        registry.add(
                "spring.jpa.properties.hibernate.hbm2ddl.extra_physical_table_types",
                () -> "PARTITIONED TABLE"
        );

        registry.add(
                "spring.jpa.properties.hibernate.type.preferred_instant_jdbc_type",
                () -> "TIMESTAMP"
        );

        registry.add(
                "spring.liquibase.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.liquibase.user",
                postgres::getUsername
        );

        registry.add(
                "spring.liquibase.password",
                postgres::getPassword
        );
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        ReconResultRepository reconResultRepository() {
            return result -> result;
        }
    }

    @Test
    void containerIsRunning() {
        // Passing proves the container and Spring context started.
    }
}
