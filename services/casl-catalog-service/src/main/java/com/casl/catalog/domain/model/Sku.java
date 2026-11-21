package com.casl.catalog.domain.model;

import com.casl.catalog.domain.exception.InvalidProductException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * CÃƒÂ³digo de producto normalizado (SKU). Inmutable.
 */
public record Sku(String value) implements java.io.Serializable {

	private static final int MAX_LENGTH = 64;
	private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$");

	public Sku {
		if (value == null || value.isBlank()) {
			throw new InvalidProductException("sku is required");
		}
		String normalized = value.trim().toUpperCase(Locale.ROOT);
		if (normalized.length() > MAX_LENGTH || !VALID.matcher(normalized).matches()) {
			throw new InvalidProductException("sku must have at most 64 alphanumeric characters, dashes or underscores");
		}
		value = normalized;
	}

	public static Sku of(String value) {
		return new Sku(value);
	}
}
