package com.casl.users.domain.model;

import com.casl.users.domain.exception.InvalidUserException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Email como value object: se normaliza (trim + minúsculas) en la construcción,
 * por lo que dos emails equivalentes son el mismo valor y la unicidad en base
 * de datos no depende de la capitalización.
 */
public record Email(String value) {

	private static final int MAX_LENGTH = 320;
	private static final Pattern VALID = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

	public Email {
		if (value == null || value.isBlank()) {
			throw new InvalidUserException("email is required");
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		if (normalized.length() > MAX_LENGTH || !VALID.matcher(normalized).matches()) {
			throw new InvalidUserException("email must be a valid address");
		}
		value = normalized;
	}

	public static Email of(String value) {
		return new Email(value);
	}
}
