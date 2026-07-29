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

    /**
    * Creates a new builder for a bond trade.
    *
    * @return a new bond trade builder
    */
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

    /**
    * Returns the bond ISIN.
    *
    * @return the bond identifier
    */
    public String isin() {
        return isin;
    }

    /**
    * Returns the bond face value.
    *
    * @return the face value
    */
    public BigDecimal faceValue() {
        return faceValue;
    }


    /**
    * Returns the bond coupon rate.
    *
    * @return the coupon rate
    */
    public BigDecimal couponRate() {
        return couponRate;
    }

    /**
    * Returns the bond maturity date.
    *
    * @return the maturity date
    */
    public LocalDate maturityDate() {
        return maturityDate;
    }


    /**
    * Returns the bond currency.
    *
    * @return the trade currency
    */
    public Currency currency() {
        return currency;
    }

    /**
    * Returns the trade side.
    *
    * @return the buy or sell side
    */
    public Side side() {
        return side;
    }

    /**
    * Returns the internal counterparty identifier.
    *
    * @return the counterparty identifier
    */
    public long counterpartyId() {
        return counterpartyId;
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

    /**
    * Builds validated bond trades.
    */
    @Override
    public boolean equals(Object o) {
        return (o instanceof BondTrade other)
                && tradeRef.equals(other.tradeRef);
    }

    @Override
    public int hashCode() {
        return tradeRef.hashCode();
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

       /**
     * Creates an empty bond trade builder.
     */
    public Builder() {
    }

    /**
     * Sets the trade reference.
     *
    * @param value the trade reference
     * @return this builder
    */
    public Builder tradeRef(TradeRef value) {
            this.tradeRef = value;
            return this;
        }

        /**
     * Sets the bond ISIN.
     *
     * @param value the bond identifier
     * @return this builder
     */
    public Builder isin(String value) {
            this.isin = value;
            return this;
        }

        /**
     * Sets the bond face value.
     *
     * @param value the face value
     * @return this builder
     */
    public Builder faceValue(BigDecimal value) {
            this.faceValue = value;
            return this;
        }

        /**
     * Sets the bond coupon rate.
     *
    * @param value the coupon rate
     * @return this builder
     */
    public Builder couponRate(BigDecimal value) {
            this.couponRate = value;
            return this;
        }

        /**
     * Sets the bond maturity date.
     *
     * @param value the maturity date
     * @return this builder
     */
    public Builder maturityDate(LocalDate value) {
            this.maturityDate = value;
            return this;
        }

        /**
     * Sets the bond currency.
     *
     * @param code the ISO currency code
     * @return this builder
     */
    public Builder currency(String code) {
            this.currency = Currency.getInstance(code);
            return this;
        }

        /**
     * Sets the trade side.
     *
     * @param value the buy or sell side
     * @return this builder
    */
    public Builder side(Side value) {
            this.side = value;
            return this;
        }

        /**
     * Sets the trade date.
    *
     * @param value the trade date
     * @return this builder
    */
    public Builder tradeDate(LocalDate value) {
            this.tradeDate = value;
            return this;
        }

    /**
     * Sets the internal counterparty identifier.
     *
     * @param value the counterparty identifier
     * @return this builder
     */
        public Builder counterpartyId(long value) {
            this.counterpartyId = value;
            return this;
        }

        /**
     * Validates the configured values and creates a bond trade.
     *
     * @return the validated bond trade
     */

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
