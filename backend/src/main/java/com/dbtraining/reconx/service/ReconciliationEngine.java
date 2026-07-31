package com.dbtraining.reconx.service;

import com.dbtraining.reconx.config.ReconConfig;
import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.BondTrade;
import com.dbtraining.reconx.model.DerivativeTrade;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.FXTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import io.micrometer.core.annotation.Timed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reconciles internal trades against an external trade feed.
 */
@Service
public class ReconciliationEngine {

    private static final AtomicInteger THREAD_NUMBER =
            new AtomicInteger(1);

    private final ExecutorService executor =
            Executors.newFixedThreadPool(
                    Math.max(
                            2,
                            Math.min(
                                    4,
                                    Runtime.getRuntime()
                                            .availableProcessors()
                            )
                    ),
                    namedThreadFactory()
            );

    private final ReconConfig reconConfig;

    /**
     * Preserves all existing direct unit-test construction.
     */
    public ReconciliationEngine() {
        this.reconConfig = null;
    }

    /**
     * Spring uses the managed runtime controls in the application context.
     */
    @Autowired
    public ReconciliationEngine(ReconConfig reconConfig) {
        this.reconConfig = reconConfig;
    }

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

        List<TradeType> safeExternal =
                external == null ? List.of() : external;

        Map<String, TradeType> externalByRef =
                safeExternal.stream()
                        .collect(Collectors.toMap(
                                trade ->
                                        trade.tradeRef().value(),
                                Function.identity(),
                                (first, duplicate) -> first
                        ));

        return internal.parallelStream()
                .map(internalTrade ->
                        matchOne(
                                internalTrade,
                                externalByRef.get(
                                        internalTrade
                                                .tradeRef()
                                                .value()
                                ),
                                rule
                        )
                )
                .toList();
    }

    public CompletableFuture<List<ReconResult>>
    reconcileByCounterparty(
            Map<Long, List<TradeType>> internalByCp,
            Map<Long, List<TradeType>> externalByCp,
            ReconciliationRule rule
    ) {
        Objects.requireNonNull(rule, "rule");

        Map<Long, List<TradeType>> safeInternal =
                internalByCp == null ? Map.of() : internalByCp;

        Map<Long, List<TradeType>> safeExternal =
                externalByCp == null ? Map.of() : externalByCp;

        Set<Long> counterpartyIds = new HashSet<>();
        counterpartyIds.addAll(safeInternal.keySet());
        counterpartyIds.addAll(safeExternal.keySet());

        List<CompletableFuture<List<ReconResult>>> futures =
                counterpartyIds.stream()
                        .map(counterpartyId ->
                                CompletableFuture.supplyAsync(
                                        () -> reconcile(
                                                safeInternal
                                                        .getOrDefault(
                                                                counterpartyId,
                                                                List.of()
                                                        ),
                                                safeExternal
                                                        .getOrDefault(
                                                                counterpartyId,
                                                                List.of()
                                                        ),
                                                rule
                                        ),
                                        executor
                                )
                        )
                        .toList();

        CompletableFuture<Void> allCompleted =
                CompletableFuture.allOf(
                        futures.toArray(
                                CompletableFuture[]::new
                        )
                );

        return allCompleted.thenApply(ignored ->
                futures.stream()
                        .flatMap(future ->
                                future.join().stream()
                        )
                        .toList()
        );
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

        if (matches(
                internalPair[0],
                internalPair[1],
                externalPair[0],
                externalPair[1],
                rule
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

    private boolean matches(
            BigDecimal internalPrice,
            BigDecimal internalQuantity,
            BigDecimal externalPrice,
            BigDecimal externalQuantity,
            ReconciliationRule rule
    ) {
        if (reconConfig == null
                || !reconConfig.hasPriceToleranceOverride()) {
            return rule.matches(
                    internalPrice,
                    internalQuantity,
                    externalPrice,
                    externalQuantity
            );
        }

        BigDecimal priceDifference =
                internalPrice.subtract(externalPrice).abs();

        BigDecimal priceDifferencePercent =
                internalPrice.signum() == 0
                        ? (
                            externalPrice.signum() == 0
                                    ? BigDecimal.ZERO
                                    : BigDecimal.ONE
                        )
                        : priceDifference.divide(
                                internalPrice.abs(),
                                12,
                                RoundingMode.HALF_UP
                        );

        BigDecimal quantityDifference =
                internalQuantity
                        .subtract(externalQuantity)
                        .abs();

        BigDecimal runtimeTolerance =
                BigDecimal.valueOf(
                        reconConfig.getPriceTolerance()
                );

        return priceDifferencePercent
                .compareTo(runtimeTolerance) <= 0
                && quantityDifference
                .compareTo(rule.qtyToleranceAbs()) <= 0;
    }

    private BigDecimal[] priceQty(TradeType trade) {
        return switch (trade) {
            case EquityTrade equity ->
                    new BigDecimal[]{
                            equity.price(),
                            equity.quantity()
                    };
            case FXTrade fx ->
                    new BigDecimal[]{
                            fx.fxRate(),
                            fx.notionalCcy1()
                    };
            case BondTrade bond ->
                    new BigDecimal[]{
                            bond.couponRate(),
                            bond.faceValue()
                    };
            case DerivativeTrade derivative ->
                    new BigDecimal[]{
                            derivative.strike(),
                            derivative.quantity()
                    };
        };
    }

    public void shutdown() {
        executor.shutdown();
    }

    private static ThreadFactory namedThreadFactory() {
        return task -> {
            Thread thread = new Thread(
                    task,
                    "recon-counterparty-"
                            + THREAD_NUMBER.getAndIncrement()
            );
            thread.setDaemon(true);
            return thread;
        };
    }
}
