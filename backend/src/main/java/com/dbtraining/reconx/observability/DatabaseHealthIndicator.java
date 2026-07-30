package com.dbtraining.reconx.observability;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

/**
 * TICKET-ADV059 — Custom database health indicator.
 *
 * Executes SELECT 1 with a two-second query timeout and reports
 * the query and elapsed execution time through Spring Boot Actuator.
 */
@Component("reconxDatabase")
public class DatabaseHealthIndicator extends AbstractHealthIndicator {

    private static final String HEALTH_QUERY = "SELECT 1";
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        super("ReconX database health check failed");
        this.dataSource = dataSource;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        long start = System.nanoTime();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.setQueryTimeout((int) TIMEOUT.toSeconds());

            try (ResultSet resultSet =
                         statement.executeQuery(HEALTH_QUERY)) {
                resultSet.next();
            }

            long elapsedMs =
                    (System.nanoTime() - start) / 1_000_000;

            builder.up()
                    .withDetail("query", HEALTH_QUERY)
                    .withDetail("elapsedMs", elapsedMs);

        } catch (SQLException exception) {
            builder.down(exception)
                    .withDetail("query", HEALTH_QUERY);
        }
    }
}