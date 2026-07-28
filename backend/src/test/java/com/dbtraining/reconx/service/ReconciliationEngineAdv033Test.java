package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.model.TradeType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class ReconciliationEngineAdv033Test {

    private final ReconciliationEngine engine = new ReconciliationEngine();

    @Test
    void exactMatchReturnsMatched() {
        List<TradeType> internal = List.of(
                equity("EQU-20260728-0001", "100.00", "10")
        );
        List<TradeType> external = List.of(
                equity("EQU-20260728-0001", "100.00", "10")
        );

        List<ReconResult> results = engine.reconcile(
                internal,
                external,
                ReconciliationRule.EXACT
        );

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().status())
                .isEqualTo(ReconResult.Status.MATCHED);
        assertThat(results.getFirst().tradeRef())
                .isEqualTo("EQU-20260728-0001");
    }

    @Test
    void missingExternalReturnsBreak() {
        List<ReconResult> results = engine.reconcile(
                List.of(equity("EQU-20260728-0002", "100.00", "10")),
                List.of(),
                ReconciliationRule.EXACT
        );

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().status())
                .isEqualTo(ReconResult.Status.BREAK);
        assertThat(results.getFirst().discrepancyType())
                .isEqualTo("MISSING_EXTERNAL");
    }

    @Test
    void nullOrEmptyInternalReturnsEmpty() {
        assertThat(engine.reconcile(
                null,
                List.of(),
                ReconciliationRule.EXACT
        )).isEmpty();

        assertThat(engine.reconcile(
                List.of(),
                null,
                ReconciliationRule.EXACT
        )).isEmpty();
    }

    @Test
    void tenThousandTradesCompleteWithinTwoSeconds() {
        List<TradeType> internal = new ArrayList<>(10_000);
        List<TradeType> external = new ArrayList<>(10_000);

        for (int index = 0; index < 10_000; index++) {
            String tradeRef = "EQU-20260728-%04d".formatted(index);
            internal.add(equity(tradeRef, "100.00", "10"));
            external.add(equity(tradeRef, "100.00", "10"));
        }

        List<ReconResult> results = assertTimeout(
                Duration.ofSeconds(2),
                () -> engine.reconcile(
                        internal,
                        external,
                        ReconciliationRule.EXACT
                )
        );

        assertThat(results).hasSize(10_000);
        assertThat(results)
                .allMatch(result -> result.status() == ReconResult.Status.MATCHED);
    }

    private EquityTrade equity(String tradeRef, String price, String quantity) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(tradeRef))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(quantity))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 7, 28))
                .counterpartyId(1L)
                .build();
    }
}
