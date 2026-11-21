package com.casl.catalog.domain.model;

import com.casl.catalog.domain.exception.InvalidProductException;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * Dinero como value object: cantidad no negativa con moneda explÃ­cita.
 */
public record Money(BigDecimal amount, Currency currency) implements java.io.Serializable {

	public Money {
		Objects.requireNonNull(amount, "amount is required");
		Objects.requireNonNull(currency, "currency is required");
		if (amount.compareTo(BigDecimal.ZERO) < 0) {
			throw new InvalidProductException("price must not be negative");
		}
		amount = amount.setScale(2, java.math.RoundingMode.HALF_UP);
	}

	public static Money of(BigDecimal amount, String currencyCode) {
		return new Money(amount, Currency.getInstance(currencyCode.toUpperCase(Locale.ROOT)));
	}

	public static Money of(double amount, String currencyCode) {
		return of(BigDecimal.valueOf(amount), currencyCode);
	}

	public Money multiply(int factor) {
		return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
	}
}
