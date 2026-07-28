package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * TICKET-ADV020 - FXTrade with Builder pattern
 *
 * WHAT:    FX trade containing two ISO-4217 currencies, a base-currency
 *          notional, and an FX rate.
 * HOW:     The builder validates required fields and FX-specific invariants.
 * WHY:     Currency is represented by java.util.Currency so invalid external
 *          codes fail at the boundary.
 * ============================================================================
 */
public final class FXTrade implements TradeType {

    private final TradeRef tradeRef;
    private final Currency ccy1;
    private final Currency ccy2;
    private final BigDecimal notionalCcy1;
    private final BigDecimal fxRate;
    private final Side side;
    private final LocalDate tradeDate;
    private final long counterpartyId;

    private FXTrade(Builder builder) {
        this.tradeRef = builder.tradeRef;
        this.ccy1 = builder.ccy1;
        this.ccy2 = builder.ccy2;
        this.notionalCcy1 = builder.notionalCcy1;
        this.fxRate = builder.fxRate;
        this.side = builder.side;
        this.tradeDate = builder.tradeDate;
        this.counterpartyId = builder.counterpartyId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public TradeRef tradeRef() {
        return tradeRef;
    }

    /**
     * Returns the converted notional in ccy2.
     * The stored {@code notionalCcy1} field is always denominated in ccy1.
     */
    @Override
    public Money notional() {
        return new Money(notionalCcy1.multiply(fxRate), ccy2);
    }

    @Override
    public LocalDate tradeDate() {
        return tradeDate;
    }

    @Override
    public AssetClass assetClass() {
        return AssetClass.FX;
    }

    public Currency ccy1() {
        return ccy1;
    }

    public Currency ccy2() {
        return ccy2;
    }

    /** Base-currency notional, denominated in ccy1. */
    public BigDecimal notionalCcy1() {
        return notionalCcy1;
    }

    public BigDecimal fxRate() {
        return fxRate;
    }

    public Side side() {
        return side;
    }

    public long counterpartyId() {
        return counterpartyId;
    }

    @Override
    public boolean equals(Object other) {
        // TODO(TICKET-ADV028): pattern-match on FXTrade and compare tradeRef.
        throw new UnsupportedOperationException("TICKET-ADV028");
    }

    @Override
    public int hashCode() {
        // TODO(TICKET-ADV028): hash from tradeRef.
        throw new UnsupportedOperationException("TICKET-ADV028");
    }

    @Override
    public String toString() {
	// NOTE: counterpartyId is deliberately omitted to prevent PII leakage.
        return "FXTrade{tradeRef=%s, ccy1=%s, ccy2=%s, notionalCcy1=%s, fxRate=%s}"
            .formatted(
                    tradeRef.value(),
                    ccy1.getCurrencyCode(),
                    ccy2.getCurrencyCode(),
                    notionalCcy1.toPlainString(),
                    fxRate.toPlainString()
            );
    }

    public static final class Builder {

        private TradeRef tradeRef;
        private Currency ccy1;
        private Currency ccy2;
        private BigDecimal notionalCcy1;
        private BigDecimal fxRate;
        private Side side;
        private LocalDate tradeDate;
        private long counterpartyId;

        public Builder tradeRef(TradeRef value) {
            this.tradeRef = value;
            return this;
        }

        public Builder ccy1(String code) {
            this.ccy1 = Currency.getInstance(code);
            return this;
        }

        public Builder ccy2(String code) {
            this.ccy2 = Currency.getInstance(code);
            return this;
        }

        public Builder notionalCcy1(BigDecimal value) {
            this.notionalCcy1 = value;
            return this;
        }

        public Builder fxRate(BigDecimal value) {
            this.fxRate = value;
            return this;
        }

        public Builder side(Side value) {
            this.side = value;
            return this;
        }

        public Builder tradeDate(LocalDate value) {
            this.tradeDate = value;
            return this;
        }

        public Builder counterpartyId(long value) {
            this.counterpartyId = value;
            return this;
        }

        public FXTrade build() {
            Objects.requireNonNull(tradeRef, "tradeRef");
            Objects.requireNonNull(ccy1, "ccy1");
            Objects.requireNonNull(ccy2, "ccy2");
            Objects.requireNonNull(notionalCcy1, "notionalCcy1");
            Objects.requireNonNull(fxRate, "fxRate");
            Objects.requireNonNull(side, "side");
            Objects.requireNonNull(tradeDate, "tradeDate");

            if (ccy1.equals(ccy2)) {
                throw new IllegalStateException("ccy1 and ccy2 must differ");
            }
            if (notionalCcy1.signum() <= 0) {
                throw new IllegalStateException("notionalCcy1 must be > 0");
            }
            if (fxRate.signum() <= 0) {
                throw new IllegalStateException("fxRate must be > 0");
            }

            return new FXTrade(this);
        }
    }
}
