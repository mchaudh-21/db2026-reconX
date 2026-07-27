package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BondTradeTest {

    @Test
    void builder_buildsValidBondTrade() {
        BondTrade trade = validBuilder().build();

        assertThat(trade.isin()).isEqualTo("US0378331005");
        assertThat(trade.notional().amount()).isEqualByComparingTo("1000000");
        assertThat(trade.notional().currency()).isEqualTo(Currency.getInstance("USD"));
        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.BOND);
    }

    @Test
    void maturityEqualToTradeDateThrows() {
        LocalDate tradeDate = LocalDate.of(2026, 6, 3);

        assertThatThrownBy(() -> validBuilder()
                .tradeDate(tradeDate)
                .maturityDate(tradeDate)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("maturityDate must be strictly after tradeDate");
    }

    @Test
    void maturityBeforeTradeDateThrows() {
        assertThatThrownBy(() -> validBuilder()
                .maturityDate(LocalDate.of(2026, 6, 2))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("maturityDate must be strictly after tradeDate");
    }

    @Test
    void invalidIsinLengthThrows() {
        assertThatThrownBy(() -> validBuilder().isin("TOO-SHORT").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("isin must be exactly 12 characters");
    }

    @Test
    void nonPositiveFaceValueThrows() {
        assertThatThrownBy(() -> validBuilder().faceValue(BigDecimal.ZERO).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("faceValue must be > 0");
    }

    @Test
    void negativeCouponRateThrows() {
        assertThatThrownBy(() -> validBuilder()
                .couponRate(new BigDecimal("-0.01"))
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("couponRate must be >= 0");
    }

    private BondTrade.Builder validBuilder() {
        return BondTrade.builder()
                .tradeRef(TradeRef.of("BND-20260603-0001"))
                .isin("US0378331005")
                .faceValue(new BigDecimal("1000000"))
                .couponRate(new BigDecimal("0.045"))
                .maturityDate(LocalDate.of(2031, 6, 3))
                .currency("USD")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(3L);
    }
}
