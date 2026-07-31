package com.dbtraining.reconx.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class DeprecatedApiControllerTest {

    private final DeprecatedApiController controller =
            new DeprecatedApiController();

    @Test
    void deprecatedTradesEndpointReturnsGoneWithHeaders() {
        ResponseEntity<Void> response =
                controller.deprecatedTradesEndpoint();

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.GONE);

        assertThat(response.getHeaders().getFirst("Deprecation"))
                .isEqualTo("true");

        assertThat(response.getHeaders().getFirst("Sunset"))
                .isNotBlank();

        assertThat(response.getHeaders().getFirst("Link"))
                .isEqualTo(
                        "</api/v1/trades>; rel=\"successor-version\""
                );
    }
}
