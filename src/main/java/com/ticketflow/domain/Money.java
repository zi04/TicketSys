package com.ticketflow.domain;

import java.math.*;
import java.util.Objects;

public final class Money {

    private static final int DECIMAL_PLACES = 2;
    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        if (amount.stripTrailingZeros().scale() > DECIMAL_PLACES) {
            throw new IllegalArgumentException("amount cannot have more than 2 decimal places: " + amount);
        }
        this.amount = amount.setScale(DECIMAL_PLACES, RoundingMode.UNNECESSARY);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public BigDecimal amount() {
        return amount;
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money multiply(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity must not be negative: " + quantity);
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)));
    }

    public String display() {
        return amount.toPlainString() + " USD";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return display();
    }
}
