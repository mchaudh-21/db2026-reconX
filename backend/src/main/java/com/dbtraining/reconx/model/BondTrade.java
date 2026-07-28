package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * TICKET-ADV021 - BondTrade with Builder pattern
 *
 * WHAT:    Fixed-income trade containing an ISIN, face value, coupon rate,
 *          maturity date, currency, side, and trade metadata.
 * HOW:     BigDecimal is used for all numeric values and build() rejects
 *          invalid fixed-income invariants.
 * ============================================================================
 */
public final class BondTrade implements TradeType {

    private final TradeRef tradeRef;
    private final String isin;
    private final BigDecimal faceValue;
    private final BigDecimal couponRate;
    private final LocalDate maturityDate;
    private final Currency currency;
    private final Side side;
    private final LocalDate tradeDate;
    private final long counterpartyId;

    private BondTrade(Builder builder) {
        this.tradeRef = builder.tradeRef;
        this.isin = builder.isin;
        this.faceValue = builder.faceValue;
        this.couponRate = builder.couponRate;
        this.maturityDate = builder.maturityDate;
        this.currency = builder.currency;
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

    @Override
    public Money notional() {
        return new Money(faceValue, currency);
    }

    @Override
    public LocalDate tradeDate() {
        return tradeDate;
    }

    @Override
    public AssetClass assetClass() {
        return AssetClass.BOND;
    }

    public String isin() {
        return isin;
    }

    public BigDecimal faceValue() {
        return faceValue;
    }

    public BigDecimal couponRate() {
        return couponRate;
    }

    public LocalDate maturityDate() {
        return maturityDate;
    }

    public Currency currency() {
        return currency;
    }

    public Side side() {
        return side;
    }

    public long counterpartyId() {
        return counterpartyId;
    }

    @Override
    public boolean equals(Object other) {
        // TODO(TICKET-ADV028): pattern-match on BondTrade and compare tradeRef.
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
    	return "BondTrade{tradeRef=%s, isin='%s', couponRate=%s, faceValue=%s, currency=%s, maturityDate=%s}"
            .formatted(
                    tradeRef.value(),
                    isin,
                    couponRate.toPlainString(),
                    faceValue.toPlainString(),
                    currency.getCurrencyCode(),
                    maturityDate
            );
    }

    public static final class Builder {

        private TradeRef tradeRef;
        private String isin;
        private BigDecimal faceValue;
        private BigDecimal couponRate;
        private LocalDate maturityDate;
        private Currency currency;
        private Side side;
        private LocalDate tradeDate;
        private long counterpartyId;

        public Builder tradeRef(TradeRef value) {
            this.tradeRef = value;
            return this;
        }

        public Builder isin(String value) {
            this.isin = value;
            return this;
        }

        public Builder faceValue(BigDecimal value) {
            this.faceValue = value;
            return this;
        }

        public Builder couponRate(BigDecimal value) {
            this.couponRate = value;
            return this;
        }

        public Builder maturityDate(LocalDate value) {
            this.maturityDate = value;
            return this;
        }

        public Builder currency(String code) {
            this.currency = Currency.getInstance(code);
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

        public BondTrade build() {
            Objects.requireNonNull(tradeRef, "tradeRef");
            Objects.requireNonNull(isin, "isin");
            Objects.requireNonNull(faceValue, "faceValue");
            Objects.requireNonNull(couponRate, "couponRate");
            Objects.requireNonNull(maturityDate, "maturityDate");
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(side, "side");
            Objects.requireNonNull(tradeDate, "tradeDate");

            if (isin.length() != 12) {
                throw new IllegalStateException("isin must be exactly 12 characters");
            }
            if (faceValue.signum() <= 0) {
                throw new IllegalStateException("faceValue must be > 0");
            }
            if (couponRate.signum() < 0) {
                throw new IllegalStateException("couponRate must be >= 0");
            }
            if (!maturityDate.isAfter(tradeDate)) {
                throw new IllegalStateException(
                        "maturityDate must be strictly after tradeDate");
            }

            return new BondTrade(this);
        }
    }
}
