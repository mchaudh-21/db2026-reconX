package com.dbtraining.reconx.service;

import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.entity.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * ADV081 — cached instrument lookup by symbol.
 */
@Service
public class InstrumentService {

    private static final Logger log =
            LoggerFactory.getLogger(InstrumentService.class);

    private final InstrumentRepository repo;

    public InstrumentService(InstrumentRepository repo) {
        this.repo = repo;
    }

    @Cacheable(
            cacheNames = "instruments",
            key = "#symbol",
            condition = "@reconConfig.cachingEnabled"
    )
    public Instrument findBySymbol(String symbol) {
        log.info("DB hit for instrument symbol={}", symbol);

        return repo.findBySymbol(symbol)
                .orElseThrow(() ->
                        new InvalidTradeException(
                                "Unknown instrument symbol: " + symbol
                        )
                );
    }
}
