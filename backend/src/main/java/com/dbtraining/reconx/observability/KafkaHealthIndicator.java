package com.dbtraining.reconx.observability;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * TICKET-ADV060 — Conditional Kafka health indicator.
 *
 * Registers only when spring.kafka.bootstrap-servers is configured.
 * Uses Kafka AdminClient to report the cluster ID and broker count.
 */
@Component("reconxKafka")
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaHealthIndicator extends AbstractHealthIndicator {

    private static final int REQUEST_TIMEOUT_MS = 2_000;
    private static final int DEFAULT_API_TIMEOUT_MS = 3_000;
    private static final long RESULT_TIMEOUT_SECONDS = 2;

    private final String bootstrapServers;

    public KafkaHealthIndicator(
            @Value("${spring.kafka.bootstrap-servers}")
            String bootstrapServers
    ) {
        super("ReconX Kafka health check failed");
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Map<String, Object> configuration = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,
                REQUEST_TIMEOUT_MS,
                AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG,
                DEFAULT_API_TIMEOUT_MS
        );

        try (AdminClient adminClient =
                     AdminClient.create(configuration)) {

            DescribeClusterResult cluster =
                    adminClient.describeCluster();

            String clusterId = cluster.clusterId()
                    .get(
                            RESULT_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );

            int nodeCount = cluster.nodes()
                    .get(
                            RESULT_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    )
                    .size();

            builder.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount);

        } catch (Exception exception) {
            builder.down(exception);
        }
    }
}