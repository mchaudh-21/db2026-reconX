package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.BondTrade;
import com.dbtraining.reconx.model.DerivativeTrade;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.FXTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import io.micrometer.core.annotation.Timed;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reconciles internal trades against an external trade feed.
 */
@Service
public class ReconciliationEngine {

    /**
     * TICKET-ADV033 — Index the external feed once, then reconcile each
     * internal trade with a constant-time lookup.
     */
    @Timed(
            value = "reconciliation.duration",
            description = "Wall time of reconcile()",
            percentiles = {0.5, 0.95, 0.99},
            histogram = true
    )
    public List<ReconResult> reconcile(
            List<TradeType> internal,
            List<TradeType> external,
            ReconciliationRule rule
    ) {
        if (internal == null || internal.isEmpty()) {
            return List.of();
        }

        Objects.requireNonNull(rule, "rule");

        List<TradeType> safeExternal = external == null ? List.of() : external;

        Map<String, TradeType> externalByRef = safeExternal.stream()
                .collect(Collectors.toMap(
                        trade -> trade.tradeRef().value(),
                        Function.identity(),
                        (first, duplicate) -> first
                ));

        return internal.parallelStream()
                .map(internalTrade -> matchOne(
                        internalTrade,
                        externalByRef.get(internalTrade.tradeRef().value()),
                        rule
                ))
                .toList();
    }

    /**
     * TICKET-ADV037 belongs to the teammate assigned parallel reconciliation.
     * It intentionally remains unimplemented on this ADV033–ADV036 branch.
     */
    public CompletableFuture<List<ReconResult>> reconcileByCounterparty(
            Map<Long, List<TradeType>> internalByCp,
            Map<Long, List<TradeType>> externalByCp,
            ReconciliationRule rule
    ) {
        throw new UnsupportedOperationException("TICKET-ADV037");
    }

    private ReconResult matchOne(
            TradeType internal,
            TradeType external,
            ReconciliationRule rule
    ) {
        String tradeRef = internal.tradeRef().value();

        if (external == null) {
            return ReconResult.breakResult(
                    tradeRef,
                    "MISSING_EXTERNAL",
                    "No external trade found for " + tradeRef
            );
        }

        BigDecimal[] internalPair = priceQty(internal);
        BigDecimal[] externalPair = priceQty(external);

        if (rule.matches(
                internalPair[0],
                internalPair[1],
                externalPair[0],
                externalPair[1]
        )) {
            return ReconResult.matched(tradeRef);
        }

        return ReconResult.breakResult(
                tradeRef,
                "VALUE_MISMATCH",
                "internal=%s/%s external=%s/%s".formatted(
                        internalPair[0],
                        internalPair[1],
                        externalPair[0],
                        externalPair[1]
                )
        );
    }

    /**
     * Exhaustive switch over the sealed TradeType hierarchy.
     */
    private BigDecimal[] priceQty(TradeType trade) {
        return switch (trade) {
            case EquityTrade equity ->
                    new BigDecimal[]{equity.price(), equity.quantity()};
            case FXTrade fx ->
                    new BigDecimal[]{fx.fxRate(), fx.notionalCcy1()};
            case BondTrade bond ->
                    new BigDecimal[]{bond.couponRate(), bond.faceValue()};
            case DerivativeTrade derivative ->
                    new BigDecimal[]{derivative.strike(), derivative.quantity()};
        };
    }
}
