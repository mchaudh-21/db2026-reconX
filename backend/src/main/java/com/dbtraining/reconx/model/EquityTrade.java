package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

/**
 * ============================================================================
 * TICKET-ADV019 - EquityTrade with Builder pattern
 *
 * WHAT:    Concrete TradeType for equity (cash share) trades.
 * HOW:     Final class, all fields final, no setters. Construction is via the
 *          nested {@link Builder}, which validates in {@link Builder#build()}.
 * WHY:     Eight required fields on a single constructor is unreadable at the
 *          call site. Builder gives named arguments, makes validation a single
 *          chokepoint, and leaves the resulting object immutable.
 * ============================================================================
 *
 * TICKET-ADV028 - equals/hashCode based on tradeRef
 * TICKET-ADV030 - PII-safe toString
 */
public final class EquityTrade implements TradeType {

    private final TradeRef tradeRef;
    private final String instrumentSymbol;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private final Currency currency;
    private final Side side;
    private final LocalDate tradeDate;
    private final long counterpartyId;

    private EquityTrade(Builder builder) {
        this.tradeRef = builder.tradeRef;
        this.instrumentSymbol = builder.instrumentSymbol;
        this.quantity = builder.quantity;
        this.price = builder.price;
        this.currency = builder.currency;
        this.side = builder.side;
        this.tradeDate = builder.tradeDate;
        this.counterpartyId = builder.counterpartyId;
    }

/**
 * Creates a new builder for an equity trade.
 *
 * @return a new equity trade builder
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
        return new Money(quantity.multiply(price), currency);
    }

    @Override
    public LocalDate tradeDate() {
        return tradeDate;
    }

    @Override
    public AssetClass assetClass() {
        return AssetClass.EQUITY;
    }
/**
 * Returns the equity instrument symbol.
 * @return the instrument symbol
 */
    public String instrumentSymbol() {
        return instrumentSymbol;
    }

/**
 * Returns the trade quantity.
 * @return the trade quantity
 */
    public BigDecimal quantity() {
        return quantity;
    }

/**
 * Returns the equity price.
 *
 * @return the equity price
 */
    public BigDecimal price() {
        return price;
    }
/**
 * Returns the trade currency.
 *
 * @return the equity trade currency
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

    /** equals: two EquityTrades are equal iff their tradeRef is equal. */
    @Override
    public boolean equals(Object other) {
        // TODO(TICKET-ADV028): pattern-match on EquityTrade and compare tradeRef.
        throw new UnsupportedOperationException("TICKET-ADV028");
    }

    @Override
    public int hashCode() {
        // TODO(TICKET-ADV028): hash from tradeRef so it pairs with equals().
        throw new UnsupportedOperationException("TICKET-ADV028");
    }

    @Override
    public String toString() {
        // NOTE: counterpartyId is deliberately omitted to prevent PII leakage.
    return "EquityTrade{tradeRef=%s, instrumentSymbol='%s', quantity=%s, price=%s, currency=%s, side=%s}"
            .formatted(
                    tradeRef.value(),
                    instrumentSymbol,
                    quantity.toPlainString(),
                    price.toPlainString(),
                    currency.getCurrencyCode(),
                    side
            );

    }

    /** Fluent builder. Required fields and invariants are checked in build(). */
    public static final class Builder {

        private TradeRef tradeRef;
        private String instrumentSymbol;
        private BigDecimal quantity;
        private BigDecimal price;
        private Currency currency;
        private Side side;
        private LocalDate tradeDate;
        private long counterpartyId;

	/**
 * Creates an empty equity trade builder.
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
 * Returns the equity instrument symbol.
 * @param value the instrument symbol
 * @return the instrument symbol
 */
        public Builder instrumentSymbol(String value) {
            this.instrumentSymbol = value;
            return this;
        }

/**
 * Sets the trade quantity.
 *
 * @param value the trade quantity
 * @return this builder
 */
        public Builder quantity(BigDecimal value) {
            this.quantity = value;
            return this;
        }
/**
 * Sets the equity price.
 *
 * @param value the equity price
 * @return this builder
 */
        public Builder price(BigDecimal value) {
            this.price = value;
            return this;
        }
/**
 * Returns the trade currency.
* @param value the trade currency
 * @return the equity trade currency
 */
        public Builder currency(Currency value) {
            this.currency = value;
            return this;
        }
/**
 * Sets the trade currency from its currency code.
 * @param code the ISO currency code
 * @return this builder
 */
        public Builder currency(String code) {
            return currency(Currency.getInstance(code));
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
 * Returns the internal counterparty identifier.
 * @param value the counterparty identifier
 * @return the counterparty identifier
 */
        public Builder counterpartyId(long value) {
            this.counterpartyId = value;
            return this;
        }
/**
 * Validates the configured values and creates an equity trade.
 *
 * @return the validated equity trade
 */
        public EquityTrade build() {
            Objects.requireNonNull(tradeRef, "tradeRef");
            Objects.requireNonNull(instrumentSymbol, "instrumentSymbol");
            Objects.requireNonNull(quantity, "quantity");
            Objects.requireNonNull(price, "price");
            Objects.requireNonNull(currency, "currency");
            Objects.requireNonNull(side, "side");
            Objects.requireNonNull(tradeDate, "tradeDate");

            if (instrumentSymbol.isBlank()) {
                throw new IllegalStateException("instrumentSymbol must not be blank");
            }
            if (quantity.signum() <= 0) {
                throw new IllegalStateException("quantity must be > 0");
            }
            if (price.signum() <= 0) {
                throw new IllegalStateException("price must be > 0");
            }

            return new EquityTrade(this);
        }
    }
}
