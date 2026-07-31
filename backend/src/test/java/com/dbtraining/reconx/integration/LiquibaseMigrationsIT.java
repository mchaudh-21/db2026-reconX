package com.dbtraining.reconx.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class LiquibaseMigrationsIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void liquibaseAppliesAllChangesetsAndLoadsSeedData() {
        Integer changelogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog",
                Integer.class
        );

        Integer activeTradeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trades WHERE deleted_at IS NULL",
                Integer.class
        );

        assertThat(changelogCount).isGreaterThanOrEqualTo(13);
        assertThat(activeTradeCount).isGreaterThanOrEqualTo(10);
    }
}
