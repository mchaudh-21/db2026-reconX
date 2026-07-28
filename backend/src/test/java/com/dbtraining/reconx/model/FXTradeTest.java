package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FXTradeTest {

    @Test
    void builder_buildsValidFxTradeAndConvertsNotionalToCcy2() {
        FXTrade trade = validBuilder().build();

        assertThat(trade.ccy1()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(trade.ccy2()).isEqualTo(Currency.getInstance("USD"));
        assertThat(trade.notional().amount()).isEqualByComparingTo("108000.0000");
        assertThat(trade.notional().currency()).isEqualTo(Currency.getInstance("USD"));
        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.FX);
    }

    @Test
    void invalidIsoCurrencyCodeThrowsAtSetter() {
        assertThatThrownBy(() -> FXTrade.builder().ccy1("EURR"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalCurrenciesThrowAtBuild() {
        assertThatThrownBy(() -> validBuilder().ccy2("EUR").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ccy1 and ccy2 must differ");
    }

    @Test
    void nonPositiveFxRateThrowsAtBuild() {
        assertThatThrownBy(() -> validBuilder().fxRate(BigDecimal.ZERO).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fxRate must be > 0");
    }

    @Test
    void nonPositiveBaseNotionalThrowsAtBuild() {
        assertThatThrownBy(() -> validBuilder().notionalCcy1(BigDecimal.ZERO).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("notionalCcy1 must be > 0");
    }

    private FXTrade.Builder validBuilder() {
        return FXTrade.builder()
                .tradeRef(TradeRef.of("FXS-20260603-0001"))
                .ccy1("EUR")
                .ccy2("USD")
                .notionalCcy1(new BigDecimal("100000"))
                .fxRate(new BigDecimal("1.0800"))
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(2L);
    }
}
