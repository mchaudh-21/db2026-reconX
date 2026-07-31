package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.dto.ReconResult;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Concrete implementation for the pre-existing ReconResultRepository contract.
 *
 * This fixes the current develop-branch application-context failure without
 * altering the interface or ReconciliationService.
 */
@Repository
public class InMemoryReconResultRepository
        implements ReconResultRepository {

    private final ConcurrentMap<String, ReconResult> results =
            new ConcurrentHashMap<>();

    @Override
    public ReconResult save(ReconResult result) {
        ReconResult saved = Objects.requireNonNull(
                result,
                "result"
        );

        results.put(saved.tradeRef(), saved);
        return saved;
    }
}
