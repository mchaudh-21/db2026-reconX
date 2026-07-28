package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV037 / ADV040 / ADV041 / ADV042 — TDD: write the test FIRST,
 * then the implementation.
 */
class ReconciliationEngineTest {

    private final ReconciliationEngine engine = new ReconciliationEngine();

    @Test
    @DisplayName("Exact matching trades return a matched reconciliation result")
    void testReconcile_exactMatch_returnsMatched() {
        // given
        EquityTrade internalTrade =
                equity("EQT-20260603-0001", "100.00", "10");
        EquityTrade externalTrade =
                equity("EQT-20260603-0001", "100.00", "10");

        // when
        List<ReconResult> out = engine.reconcile(
                List.of(internalTrade),
                List.of(externalTrade),
                ReconciliationRule.EXACT
        );

        // then
        assertThat(out).hasSize(1);
        assertThat(out.getFirst().status())
                .isEqualTo(ReconResult.Status.MATCHED);
    }

        @ParameterizedTest(
                name = "price diff {0} stays within 1% tolerance -> MATCHED"
        )
        @ValueSource(strings = {"0.10", "0.50", "0.99"})
        void testReconcile_priceTolerance_withinThreshold(String diff) {
        BigDecimal basePrice = new BigDecimal("100.00");

        EquityTrade internal = equity(
                "EQU-20260603-0002",
                basePrice.toPlainString(),
                "1000"
        );

        EquityTrade external = equity(
                "EQU-20260603-0002",
                basePrice.add(new BigDecimal(diff)).toPlainString(),
                "1000"
        );

        List<ReconResult> out = engine.reconcile(
                List.of(internal),
                List.of(external),
                ReconciliationRule.PRICE_TOLERANCE_1PCT
        );

        assertThat(out.get(0).status())
                .isEqualTo(ReconResult.Status.MATCHED);
        }

        @Test
        void testReconcile_missingCounterpartyTrade_returnsBreak() {
        // Given
        EquityTrade internal = equity(
                "EQU-20260603-0003",
                "100.00",
                "1000"
        );

        // When
        List<ReconResult> out = engine.reconcile(
                List.of(internal),
                List.of(),
                ReconciliationRule.EXACT
        );

        // Then
        assertThat(out.get(0).status())
                .isEqualTo(ReconResult.Status.BREAK);

        assertThat(out.get(0).discrepancyType())
                .isEqualTo("MISSING_EXTERNAL");
        }

    @Test
    @DisplayName("Empty internal trades return an empty reconciliation result")
    void testReconcile_emptyInternal_returnsEmpty() {
        // given
        List<TradeType> internal = List.of();
        List<TradeType> external = List.of();

        // when
        List<ReconResult> out = engine.reconcile(
                internal,
                external,
                ReconciliationRule.EXACT
        );

        // then
        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("Trades are reconciled concurrently by counterparty")
    void testReconcileByCounterparty_returnsCombinedResults() {
        // given
        EquityTrade internalOne =
                equity("EQT-20260603-0001", "100.00", "10");
        EquityTrade externalOne =
                equity("EQT-20260603-0001", "100.00", "10");

        EquityTrade internalTwo =
                equity("EQT-20260603-0002", "200.00", "20");
        EquityTrade externalTwo =
                equity("EQT-20260603-0002", "200.00", "20");

        Map<Long, List<TradeType>> internalByCp = Map.of(
                1L, List.of(internalOne),
                2L, List.of(internalTwo)
        );

        Map<Long, List<TradeType>> externalByCp = Map.of(
                1L, List.of(externalOne),
                2L, List.of(externalTwo)
        );

        // when
        List<ReconResult> out = engine.reconcileByCounterparty(
                internalByCp,
                externalByCp,
                ReconciliationRule.EXACT
        ).join();

        // then
        assertThat(out)
                .hasSize(2)
                .allSatisfy(result ->
                        assertThat(result.status())
                                .isEqualTo(ReconResult.Status.MATCHED)
                );
    }

    private EquityTrade equity(String ref, String price, String qty) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(qty))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}