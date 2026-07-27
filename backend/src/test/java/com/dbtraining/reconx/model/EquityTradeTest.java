package com.dbtraining.reconx.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EquityTradeTest {

    @Test
    void builder_buildsWhenAllRequiredPresent() {
        EquityTrade trade = sampleEquity("EQU-20260603-0001");

        assertThat(trade.tradeRef()).isEqualTo(TradeRef.of("EQU-20260603-0001"));
        assertThat(trade.notional().amount()).isEqualByComparingTo("10000");
        assertThat(trade.notional().currency()).isEqualTo(Currency.getInstance("EUR"));
        assertThat(trade.assetClass()).isEqualTo(TradeType.AssetClass.EQUITY);
    }

    @Test
    void builder_missingPrice_throws() {
        assertThatThrownBy(() -> EquityTrade.builder()
                .tradeRef(TradeRef.of("EQU-20260603-0002"))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("price");
    }

    @Test
    void builder_nonPositiveQuantity_throws() {
        assertThatThrownBy(() -> EquityTrade.builder()
                .tradeRef(TradeRef.of("EQU-20260603-0003"))
                .instrumentSymbol("SAP.DE")
                .quantity(BigDecimal.ZERO)
                .price(new BigDecimal("100"))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("quantity must be > 0");
    }

    @Test
    void builder_blankInstrumentSymbol_throws() {
        assertThatThrownBy(() -> EquityTrade.builder()
                .tradeRef(TradeRef.of("EQU-20260603-0004"))
                .instrumentSymbol(" ")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("100"))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("instrumentSymbol must not be blank");
    }

    @Test
    void equality_byTradeRef() {
        // TODO(TICKET-ADV028): two EquityTrades with the same tradeRef are
        // equal and share hashCode; a third with a different ref is not equal.
        org.junit.jupiter.api.Assertions.fail("TICKET-ADV028 not implemented yet");
    }

    private EquityTrade sampleEquity(String ref) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .quantity(new BigDecimal("100"))
                .price(new BigDecimal("100"))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}
