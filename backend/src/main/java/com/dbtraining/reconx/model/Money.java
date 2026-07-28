package com.dbtraining.reconx.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");

        if (amount.signum() < 0) {
            throw new IllegalArgumentException(
                    "Money amount cannot be negative: " + amount
            );
        }
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(
                new BigDecimal(amount),
                Currency.getInstance(currencyCode)
        );
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(
                amount,
                Currency.getInstance(currencyCode)
        );
    }

    /** Add another Money of the same currency. Throws on currency mismatch. */
    public Money plus(Money other) {
        Objects.requireNonNull(other, "other");

        if (!currency.equals(other.currency())) {
            throw new IllegalArgumentException(
                    "Cannot add different currencies: "
                            + currency.getCurrencyCode()
                            + " and "
                            + other.currency().getCurrencyCode()
            );
        }

        return new Money(amount.add(other.amount()), currency);
    }

    public Money times(BigDecimal multiplier) {
        Objects.requireNonNull(multiplier, "multiplier");
        return new Money(amount.multiply(multiplier), currency);
    }
}