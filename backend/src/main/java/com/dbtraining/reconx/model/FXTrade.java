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

/**
 * Creates a new builder for an FX trade.
 *
 * @return a new FX trade builder
 */
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

/**
 * Returns the first trade currency.
 *
 * @return the first currency
 */
    public Currency ccy1() {
        return ccy1;
    }

/**
 * Returns the second trade currency.
 *
 * @return the second currency
 */
    public Currency ccy2() {
        return ccy2;
    }

/**
 * Returns the base-currency notional denominated in currency one.
 *
 * @return the notional amount in currency one
 */
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
/**
 * Fluent builder for creating FX trades.
 */

    @Override
    public boolean equals(Object o) {
        return (o instanceof FXTrade other)
                && tradeRef.equals(other.tradeRef);
    }

    @Override
    public int hashCode() {
        return tradeRef.hashCode();
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


/**
 * Creates a new builder for an FX trade.
 */
	public Builder() {
	}   
        public Builder tradeRef(TradeRef value) {
            this.tradeRef = value;
            return this;
        }

/**
 * Sets the first trade currency.
 *
 * @param code the first currency code
 * @return this builder
 */
        public Builder ccy1(String code) {
            this.ccy1 = Currency.getInstance(code);
            return this;
        }



/**
 * Sets the second trade currency.
 *
 * @param code the second currency code
 * @return this builder
 */
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
