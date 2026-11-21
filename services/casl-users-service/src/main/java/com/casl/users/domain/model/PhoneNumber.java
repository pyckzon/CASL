package com.casl.users.domain.model;

import com.casl.users.domain.exception.InvalidUserException;

import java.util.regex.Pattern;

/**
 * Teléfono opcional. Un valor en blanco se normaliza a {@code null} para que
 * "sin teléfono" tenga una única representación.
 */
public record PhoneNumber(String value) {

	private static final int MAX_LENGTH = 30;
	private static final Pattern VALID = Pattern.compile("^[+0-9][0-9 ()-]{5,29}$");

	public PhoneNumber {
		String normalized = (value == null || value.isBlank()) ? null : value.trim();
		if (normalized != null && (normalized.length() > MAX_LENGTH || !VALID.matcher(normalized).matches())) {
			throw new InvalidUserException(
					"phone must have 6 to 30 characters and may only contain digits, +, spaces, parentheses or dashes");
		}
		value = normalized;
	}

	public static PhoneNumber of(String value) {
		return new PhoneNumber(value);
	}

	public boolean isPresent() {
		return value != null;
	}
}
