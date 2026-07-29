package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.ReconResultRepository;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

@Service
public class ReconciliationService {

    private final ReconciliationEngine engine;
    private final ReconResultRepository repository;

    public ReconciliationService(
            ReconciliationEngine engine,
            ReconResultRepository repository
    ) {
        this.engine = Objects.requireNonNull(engine);
        this.repository = Objects.requireNonNull(repository);
    }

    public List<ReconResult> runRecon(
            List<TradeType> internalTrades,
            List<TradeType> externalTrades,
            ReconciliationRule rule
    ) {
        List<ReconResult> results =
                engine.reconcile(internalTrades, externalTrades, rule);

        results.forEach(repository::save);

        return results;
    }
}