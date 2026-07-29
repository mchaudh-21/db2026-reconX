package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * TICKET-ADV022 — DerivativeTrade with Builder pattern
 *
 * WHAT:    Option/derivative trade — underlying, strike, expiry, optionType.
 * HOW:     Same builder pattern. notional() = strike * quantity in the
 *          trade's currency (simplified — real derivatives use delta-adjusted).
 * ============================================================================
 */
public final class DerivativeTrade implements TradeType {

    /**
    * Identifies whether the derivative is a call or put option.
    */
    public enum OptionType {
	/** Call option. */
	 CALL, 
	/** Put option. */
	PUT }

    private final TradeRef tradeRef;
    private final String underlying;
    private final BigDecimal strike;
    private final BigDecimal quantity;
    private final LocalDate expiry;
    private final OptionType optionType;
    private final Currency currency;
    private final Side side;
    private final LocalDate tradeDate;
    private final long counterpartyId;

    private DerivativeTrade(Builder b) {
        this.tradeRef = b.tradeRef;
        this.underlying = b.underlying;
        this.strike = b.strike;
        this.quantity = b.quantity;
        this.expiry = b.expiry;
        this.optionType = b.optionType;
        this.currency = b.currency;
        this.side = b.side;
        this.tradeDate = b.tradeDate;
        this.counterpartyId = b.counterpartyId;
    }

    /**
    * Creates a new builder for a derivative trade.
    *
    * @return a new derivative trade builder
    */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public TradeRef tradeRef() {
        return tradeRef;
    }

    @Override
    public LocalDate tradeDate() {
        return tradeDate;
    }

    @Override
    public AssetClass assetClass() {
        return AssetClass.DERIVATIVE;
    }

    /** Simplified notional = strike * quantity in the trade currency. */
    @Override
    public Money notional() {
        return new Money(strike.multiply(quantity), currency);
    }

    /**
    * Returns the underlying instrument identifier.
    *
    * @return the underlying instrument
    */
    public String underlying() {
        return underlying;
    }

    /**
    * Returns the derivative strike price.
    *
    * @return the strike price
    */
    public BigDecimal strike() {
        return strike;
    }

    /**
    * Returns the derivative quantity.
    *
    * @return the trade quantity
    */
    public BigDecimal quantity() {
        return quantity;
    }

    /**
    * Returns the derivative expiry date.
    *
    * @return the expiry date
    */
    public LocalDate expiry() {
        return expiry;
    }

    /**
    * Returns whether the derivative is a call or put option.
    *
    * @return the option type
    */
    public OptionType optionType() {
        return optionType;
    }

    /**
    * Returns the derivative trade currency.
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
    public boolean equals(Object o) {
        // TODO(TICKET-ADV028): pattern-match on DerivativeTrade and compare tradeRef.
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
        return "DerivativeTrade{tradeRef=%s, underlying='%s', strike=%s, quantity=%s, currency=%s, side=%s, expiry=%s, optionType=%s}"
            .formatted(
                    tradeRef.value(),
                    underlying,
                    strike.toPlainString(),
                    quantity.toPlainString(),
                    currency.getCurrencyCode(),
                    side,
                    expiry,
                    optionType
            );
    }

    /**
    * Builds validated derivative trades.
    */
    public static final class Builder {

        private TradeRef tradeRef;
        private String underlying;
        private BigDecimal strike;
        private BigDecimal quantity;
        private LocalDate expiry;
        private LocalDate tradeDate;
        private OptionType optionType;
        private Currency currency;
        private Side side;
        private long counterpartyId;
    /**
    * Creates an empty derivative trade builder.
    */
        public Builder() {
        }
     
 /**
 * Sets the trade reference.
 *
 * @param v the trade reference
 * @return this builder
 */
	 public Builder tradeRef(TradeRef v) {
            this.tradeRef = v;
            return this;
        }

/**
 * Sets the underlying instrument identifier.
 *
 * @param v the underlying instrument
 * @return this builder
 */  
       public Builder underlying(String v) {
            this.underlying = v;
            return this;
        }
/**
 * Sets the derivative strike price.
 *
 * @param v the strike price
 * @return this builder
 */
        public Builder strike(BigDecimal v) {
            this.strike = v;
            return this;
        }
/**
 * Sets the derivative quantity.
 *
 * @param v the trade quantity
 * @return this builder
 */
        public Builder quantity(BigDecimal v) {
            this.quantity = v;
            return this;
        }

        /**
 * Sets the derivative expiry date.
 *
 * @param v the expiry date
 * @return this builder
 */
        public Builder expiry(LocalDate v) {
            this.expiry = v;
            return this;
        }
/**
 * Sets the derivative option type.
 *
 * @param v the call or put option type
 * @return this builder
 */
        public Builder optionType(OptionType v) {
            this.optionType = v;
            return this;
        }
/**
 * Sets the derivative trade currency.
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
 * @param v the buy or sell side
 * @return this builder
 */
        public Builder side(Side v) {
            this.side = v;
            return this;
        }
/**
 * Sets the trade date.
 *
 * @param v the trade date
 * @return this builder
 */
        public Builder tradeDate(LocalDate v) {
            this.tradeDate = v;
            return this;
        }

/**
 * Sets the internal counterparty identifier.
 *
 * @param v the counterparty identifier
 * @return this builder
 */   
     public Builder counterpartyId(long v) {
            this.counterpartyId = v;
            return this;
        }

/**
 * Validates the configured values and creates a derivative trade.
 *
 * @return the validated derivative trade
 */
        public DerivativeTrade build() {
            Objects.requireNonNull(tradeRef, "tradeRef");
            Objects.requireNonNull(underlying, "underlying");
            Objects.requireNonNull(strike, "strike");
            Objects.requireNonNull(quantity, "quantity");
            Objects.requireNonNull(expiry, "expiry");
            Objects.requireNonNull(optionType, "optionType");
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(side, "side");
            Objects.requireNonNull(tradeDate, "tradeDate");

            if (underlying.isBlank()) {
                throw new IllegalArgumentException(
                        "underlying cannot be blank"
                );
            }

            if (strike.signum() <= 0) {
                throw new IllegalArgumentException(
                        "strike must be greater than zero: " + strike
                );
            }

            if (quantity.signum() <= 0) {
                throw new IllegalArgumentException(
                        "quantity must be greater than zero: " + quantity
                );
            }

            if (!expiry.isAfter(tradeDate)) {
                throw new IllegalArgumentException(
                        "expiry must be after tradeDate"
                );
            }

            return new DerivativeTrade(this);
        }
    }
}
