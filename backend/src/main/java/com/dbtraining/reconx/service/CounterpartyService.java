package com.dbtraining.reconx.service;

import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.entity.Counterparty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * ADV082 — one-minute counterparty reference-data cache.
 */
@Service
public class CounterpartyService {

    private static final Logger log =
            LoggerFactory.getLogger(CounterpartyService.class);

    private final CounterpartyRepository repository;

    public CounterpartyService(CounterpartyRepository repository) {
        this.repository = repository;
    }

    @Cacheable(
            cacheNames = "counterparties",
            key = "#leiCode",
            condition = "@reconConfig.cachingEnabled"
    )
    public Counterparty findByLeiCode(String leiCode) {
        log.info("DB hit for counterparty LEI={}", leiCode);

        return repository.findByLeiCode(leiCode)
                .orElseThrow(() ->
                        new InvalidTradeException(
                                "Unknown counterparty LEI: " + leiCode
                        )
                );
    }
}
