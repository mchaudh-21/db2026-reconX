package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.repository.entity.Instrument;
import com.dbtraining.reconx.service.InstrumentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ADV081 — endpoint used to demonstrate cold and warm symbol lookups.
 *
 * Returns a response record rather than exposing the JPA entity directly.
 */
@RestController
@RequestMapping("/v1/instruments")
@Tag(name = "instruments")
@SecurityRequirement(name = "bearerAuth")
public class InstrumentController {

    private final InstrumentService service;

    public InstrumentController(InstrumentService service) {
        this.service = service;
    }

    @GetMapping("/{symbol}")
    public InstrumentView findBySymbol(
            @PathVariable String symbol
    ) {
        Instrument instrument = service.findBySymbol(symbol);

        Map<String, Object> metadata =
                instrument.getMetadata() == null
                        ? Map.of()
                        : new LinkedHashMap<>(
                                instrument.getMetadata()
                        );

        return new InstrumentView(
                instrument.getId(),
                instrument.getSymbol(),
                instrument.getName(),
                instrument.getAssetClass().name(),
                instrument.getCurrency(),
                instrument.getIsin(),
                metadata
        );
    }

    public record InstrumentView(
            Long id,
            String symbol,
            String name,
            String assetClass,
            String currency,
            String isin,
            Map<String, Object> metadata
    ) {
    }
}