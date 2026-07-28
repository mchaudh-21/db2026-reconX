package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.EquityTrade;
import com.dbtraining.reconx.model.Side;
import com.dbtraining.reconx.model.TradeRef;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TradeAnalyticsServiceTest {

    private final TradeAnalyticsService service = new TradeAnalyticsService();

    @Test
    void notionalSummaryCarriesAllFiveAggregatesInBigDecimal() {
        List<EquityTrade> trades = List.of(
                equity("EQU-20260728-0101", "AAA", "100", "10", Side.BUY, 1L),
                equity("EQU-20260728-0102", "AAA", "300", "10", Side.BUY, 1L),
                equity("EQU-20260728-0103", "BBB", "50", "20", Side.SELL, 1L),
                equity("EQU-20260728-0104", "CCC", "25", "4", Side.BUY, 2L)
        );

        TradeAnalyticsService.NotionalSummary cp1 =
                service.notionalByCounterparty(trades).get(1L);

        assertThat(cp1.count()).isEqualTo(3);
        assertThat(cp1.total()).isEqualByComparingTo("5000");
        assertThat(cp1.min()).isEqualByComparingTo("1000");
        assertThat(cp1.max()).isEqualByComparingTo("3000");
        assertThat(cp1.average()).isEqualByComparingTo("1666.666667");
    }

    @Test
    void customVwapCollectorIsEqualForSerialAndParallelStreams() {
        List<EquityTrade> trades = List.of(
                equity("EQU-20260728-0201", "AAPL", "100", "10", Side.BUY, 1L),
                equity("EQU-20260728-0202", "AAPL", "110", "30", Side.BUY, 1L)
        );

        BigDecimal serial = trades.stream().collect(new VwapCollector());
        BigDecimal parallel = trades.parallelStream().collect(new VwapCollector());

        assertThat(serial).isEqualByComparingTo("107.500000");
        assertThat(parallel).isEqualByComparingTo(serial);
    }

    @Test
    void customVwapCollectorReturnsZeroForEmptyInput() {
        BigDecimal result = List.<EquityTrade>of()
                .stream()
                .collect(new VwapCollector());

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void vwapByInstrumentProducesExpectedValues() {
        List<EquityTrade> trades = List.of(
                equity("EQU-20260728-0301", "AAPL", "100", "10", Side.BUY, 1L),
                equity("EQU-20260728-0302", "AAPL", "110", "30", Side.BUY, 1L),
                equity("EQU-20260728-0303", "MSFT", "200", "2", Side.BUY, 1L)
        );

        var result = service.vwapByInstrument(trades);

        assertThat(result.get("AAPL")).isEqualByComparingTo("107.500000");
        assertThat(result.get("MSFT")).isEqualByComparingTo("200.000000");
    }

    @Test
    void pnlByInstrumentUsesBuyAsCostAndSellAsRevenue() {
        List<EquityTrade> trades = List.of(
                equity("EQU-20260728-0401", "AAPL", "100", "10", Side.BUY, 1L),
                equity("EQU-20260728-0402", "AAPL", "110", "5", Side.SELL, 1L),
                equity("EQU-20260728-0403", "MSFT", "200", "2", Side.SELL, 1L)
        );

        var result = service.pnlByInstrument(trades);

        assertThat(result.get("AAPL")).isEqualByComparingTo("-450");
        assertThat(result.get("MSFT")).isEqualByComparingTo("400");
    }

    @Test
    void analyticsMethodsReturnEmptyMapsForNullOrEmptyInput() {
        assertThat(service.notionalByCounterparty(null)).isEmpty();
        assertThat(service.vwapByInstrument(null)).isEmpty();
        assertThat(service.pnlByInstrument(List.of())).isEmpty();
    }

    private EquityTrade equity(
            String tradeRef,
            String symbol,
            String price,
            String quantity,
            Side side,
            long counterpartyId
    ) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(tradeRef))
                .instrumentSymbol(symbol)
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(quantity))
                .currency("USD")
                .side(side)
                .tradeDate(LocalDate.of(2026, 7, 28))
                .counterpartyId(counterpartyId)
                .build();
    }
}
