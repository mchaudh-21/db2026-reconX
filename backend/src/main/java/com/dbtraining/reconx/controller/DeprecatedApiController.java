package com.dbtraining.reconx.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeprecatedApiController {

    @Deprecated(since = "v1.4.0", forRemoval = true)
    @GetMapping("/v0/trades")
    public ResponseEntity<Void> deprecatedTradesEndpoint() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Deprecation", "true");
        headers.add("Sunset", "Wed, 31 Dec 2026 23:59:59 GMT");
        headers.add(
                "Link",
                "</api/v1/trades>; rel=\"successor-version\""
        );

        return new ResponseEntity<>(headers, HttpStatus.GONE);
    }
}
