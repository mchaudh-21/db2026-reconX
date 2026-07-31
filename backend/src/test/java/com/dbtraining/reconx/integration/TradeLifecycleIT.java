package com.dbtraining.reconx.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TradeLifecycleIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static String token;
    private static Long tradeId;
    private static Long reconBreakId;
    private static String jobId;

    @Test
    @Order(1)
    void loginAsAdmin() throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/api/auth/login"),
                Map.of(
                        "email", "admin@db.com",
                        "password", "admin123"
                ),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(response.getBody());
        token = body.get("token").asText();

        assertThat(token).isNotBlank();
    }

    @Test
    @Order(2)
    void createTrade() throws Exception {
        HttpHeaders headers = authHeaders();

        Map<String, Object> request = Map.of(
                "tradeRef", "TRD-20260731-9001",
                "instrumentId", 1,
                "counterpartyId", 1,
                "assetClass", "EQUITY",
                "side", "BUY",
                "quantity", 100.0,
                "price", 245.50,
                "tradeDate", "2026-07-30"
        );

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/trades"),
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();

        JsonNode body = objectMapper.readTree(response.getBody());
        tradeId = body.get("id").asLong();

        assertThat(tradeId).isPositive();
    }

    @Test
    @Order(3)
    void getTradeBack() {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/trades?page=0&size=20"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("TRD-20260731-9001");
    }

    @Test
    @Order(4)
    void patchStatus() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/trades/" + tradeId + "/status"),
                HttpMethod.PATCH,
                new HttpEntity<>(
                        Map.of("status", "MATCHED"),
                        authHeaders()
                ),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("status").asText()).isEqualTo("MATCHED");
    }

    @Test
    @Order(5)
    void triggerRecon() throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/recon/run"),
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of(
                                "from", "2026-07-01",
                                "to", "2026-07-31"
                        ),
                        authHeaders()
                ),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        JsonNode body = objectMapper.readTree(response.getBody());
        jobId = body.get("jobId").asText();

        assertThat(jobId).isNotBlank();
        assertThat(body.get("status").asText()).isEqualTo("QUEUED");
    }

    @Test
    @Order(6)
    void resolveBreak() throws Exception {
        ResponseEntity<String> resultsResponse = restTemplate.exchange(
                url("/api/v1/recon/jobs/" + jobId + "/results"),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                String.class
        );

        assertThat(resultsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode results = objectMapper.readTree(resultsResponse.getBody());

        if (results.isArray() && !results.isEmpty()) {
            reconBreakId = results.get(0).get("id").asLong();
        }


	assertThat(reconBreakId)
        .as("Expected at least one reconciliation break for job " + jobId)
        .isNotNull();

        ResponseEntity<String> response = restTemplate.exchange(
                url("/api/v1/recon/results/" + reconBreakId + "/resolve"),
                HttpMethod.PUT,
                new HttpEntity<>(
                        Map.of("note", "Resolved by ADV078 lifecycle test"),
                        authHeaders()
                ),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("status").asText()).isEqualTo("RESOLVED");
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
