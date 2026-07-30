package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.dto.ReconSummary;
import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import com.dbtraining.reconx.model.TradeType;
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
 * TICKET-ADV037 / ADV040 / ADV041 / ADV042 / ADV047
 *
 * Tests reconciliation behavior, tolerance rules, missing trades,
 * empty inputs, concurrent counterparty reconciliation, and summary results.
 */
class ReconciliationEngineTest {

    private final ReconciliationEngine engine = new ReconciliationEngine();

    @Test
    @DisplayName("Exact matching trades return a matched reconciliation result")
    void testReconcile_exactMatch_returnsMatched() {
        // Given
        EquityTrade internalTrade =
                equity("EQT-20260603-0001", "100.00", "10");

        EquityTrade externalTrade =
                equity("EQT-20260603-0001", "100.00", "10");

        // When
        List<ReconResult> results = engine.reconcile(
                List.of(internalTrade),
                List.of(externalTrade),
                ReconciliationRule.EXACT
        );

        // Then
        assertThat(results).hasSize(1);

        ReconResult result = results.getFirst();

        assertThat(result.tradeRef())
                .isEqualTo("EQT-20260603-0001");

        assertThat(result.status())
                .isEqualTo(ReconResult.Status.MATCHED);

        assertThat(result.discrepancyType()).isNull();
        assertThat(result.details()).isNull();
    }

    @ParameterizedTest(
            name = "Price difference {0} remains within 1% tolerance"
    )
    @ValueSource(strings = {"0.10", "0.50", "0.99"})
    @DisplayName("Price differences within one percent return matched")
    void testReconcile_priceTolerance_withinThreshold(String difference) {
        // Given
        BigDecimal basePrice = new BigDecimal("100.00");

        EquityTrade internalTrade = equity(
                "EQU-20260603-0002",
                basePrice.toPlainString(),
                "1000"
        );

        EquityTrade externalTrade = equity(
                "EQU-20260603-0002",
                basePrice.add(new BigDecimal(difference)).toPlainString(),
                "1000"
        );

        // When
        List<ReconResult> results = engine.reconcile(
                List.of(internalTrade),
                List.of(externalTrade),
                ReconciliationRule.PRICE_TOLERANCE_1PCT
        );

        // Then
        assertThat(results).hasSize(1);

        assertThat(results.getFirst().status())
                .isEqualTo(ReconResult.Status.MATCHED);
    }

    @Test
    @DisplayName("Missing external trade returns a break")
    void testReconcile_missingCounterpartyTrade_returnsBreak() {
        // Given
        EquityTrade internalTrade = equity(
                "EQU-20260603-0003",
                "100.00",
                "1000"
        );

        // When
        List<ReconResult> results = engine.reconcile(
                List.of(internalTrade),
                List.of(),
                ReconciliationRule.EXACT
        );

        // Then
        assertThat(results).hasSize(1);

        ReconResult result = results.getFirst();

        assertThat(result.tradeRef())
                .isEqualTo("EQU-20260603-0003");

        assertThat(result.status())
                .isEqualTo(ReconResult.Status.BREAK);

        assertThat(result.discrepancyType())
                .isEqualTo("MISSING_EXTERNAL");

        assertThat(result.details())
                .contains("No external trade found");
    }

    @Test
    @DisplayName("Empty internal trades return an empty reconciliation result")
    void testReconcile_emptyInternal_returnsEmpty() {
        // Given
        List<TradeType> internalTrades = List.of();
        List<TradeType> externalTrades = List.of();

        // When
        List<ReconResult> results = engine.reconcile(
                internalTrades,
                externalTrades,
                ReconciliationRule.EXACT
        );

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Null internal trades return an empty reconciliation result")
    void testReconcile_nullInternal_returnsEmpty() {
        // When
        List<ReconResult> results = engine.reconcile(
                null,
                List.of(),
                ReconciliationRule.EXACT
        );

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Null external trades are treated as an empty external feed")
    void testReconcile_nullExternal_returnsMissingExternalBreak() {
        // Given
        EquityTrade internalTrade = equity(
                "EQU-20260603-0004",
                "100.00",
                "1000"
        );

        // When
        List<ReconResult> results = engine.reconcile(
                List.of(internalTrade),
                null,
                ReconciliationRule.EXACT
        );

        // Then
        assertThat(results).hasSize(1);

        assertThat(results.getFirst().status())
                .isEqualTo(ReconResult.Status.BREAK);

        assertThat(results.getFirst().discrepancyType())
                .isEqualTo("MISSING_EXTERNAL");
    }

    @Test
    @DisplayName("Duplicate external references do not cause reconciliation to fail")
    void testReconcile_duplicateExternalReferences_usesFirstTrade() {
        // Given
        EquityTrade internalTrade = equity(
                "EQU-20260603-0005",
                "100.00",
                "10"
        );

        EquityTrade firstExternalTrade = equity(
                "EQU-20260603-0005",
                "100.00",
                "10"
        );

        EquityTrade duplicateExternalTrade = equity(
                "EQU-20260603-0005",
                "200.00",
                "20"
        );

        // When
        List<ReconResult> results = engine.reconcile(
                List.of(internalTrade),
                List.of(firstExternalTrade, duplicateExternalTrade),
                ReconciliationRule.EXACT
        );

        // Then
        assertThat(results).hasSize(1);

        assertThat(results.getFirst().status())
                .isEqualTo(ReconResult.Status.MATCHED);
    }

    @Test
    @DisplayName("Trades are reconciled concurrently by counterparty")
    void testReconcileByCounterparty_returnsCombinedResults() {
        // Given
        EquityTrade internalOne =
                equity("EQT-20260603-0010", "100.00", "10");

        EquityTrade externalOne =
                equity("EQT-20260603-0010", "100.00", "10");

        EquityTrade internalTwo =
                equity("EQT-20260603-0020", "200.00", "20");

        EquityTrade externalTwo =
                equity("EQT-20260603-0020", "200.00", "20");

        Map<Long, List<TradeType>> internalByCounterparty = Map.of(
                1L, List.of(internalOne),
                2L, List.of(internalTwo)
        );

        Map<Long, List<TradeType>> externalByCounterparty = Map.of(
                1L, List.of(externalOne),
                2L, List.of(externalTwo)
        );

        // When
        List<ReconResult> results = engine.reconcileByCounterparty(
                internalByCounterparty,
                externalByCounterparty,
                ReconciliationRule.EXACT
        ).join();

        // Then
        assertThat(results)
                .hasSize(2)
                .allSatisfy(result ->
                        assertThat(result.status())
                                .isEqualTo(ReconResult.Status.MATCHED)
                );
    }

    @Test
    @DisplayName("Three mismatched trades produce a summary with three breaks")
    void testSummary_allBroken() {
        // Given
        List<TradeType> internalTrades = List.of(
                equity("ABC-20260603-0001", "100.00", "10"),
                equity("ABC-20260603-0002", "200.00", "20"),
                equity("ABC-20260603-0003", "300.00", "30")
        );

        List<TradeType> externalTrades = List.of(
                equity("ABC-20260603-0001", "101.00", "10"),
                equity("ABC-20260603-0002", "201.00", "20"),
                equity("ABC-20260603-0003", "301.00", "30")
        );

        // When
        List<ReconResult> results = engine.reconcile(
                internalTrades,
                externalTrades,
                ReconciliationRule.EXACT
        );

        ReconSummary summary =
        results.stream()
                .collect(new ReconSummaryCollector());

        // Then
        assertThat(results)
                .hasSize(3)
                .allSatisfy(result -> {
                    assertThat(result.status())
                            .isEqualTo(ReconResult.Status.BREAK);

                    assertThat(result.discrepancyType())
                            .isEqualTo("VALUE_MISMATCH");
                });

        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.matched()).isZero();
        assertThat(summary.broken()).isEqualTo(3);
    }

    private EquityTrade equity(
            String tradeReference,
            String price,
            String quantity
    ) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(tradeReference))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(quantity))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}