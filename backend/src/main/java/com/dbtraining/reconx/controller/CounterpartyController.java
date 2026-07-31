package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.entity.Counterparty;
import com.dbtraining.reconx.service.CounterpartyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADV082 — endpoint used to exercise the one-minute counterparty cache.
 */
@RestController
@RequestMapping("/v1/counterparties")
@Tag(name = "counterparties")
@SecurityRequirement(name = "bearerAuth")
public class CounterpartyController {

    private final CounterpartyService service;

    public CounterpartyController(CounterpartyService service) {
        this.service = service;
    }

    @GetMapping("/{leiCode}")
    public Counterparty findByLeiCode(@PathVariable String leiCode) {
        return service.findByLeiCode(leiCode);
    }
}
