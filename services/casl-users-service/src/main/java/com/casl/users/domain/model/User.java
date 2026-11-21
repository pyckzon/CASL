package com.casl.users.domain.model;

import com.casl.users.domain.exception.InvalidUserException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado de usuario. Inmutable: una actualización produce una instancia
 * nueva, de modo que no hay estado compartido que se pueda mutar por accidente.
 */
public record User(
		UUID id,
		Email email,
		String firstName,
		String lastName,
		PhoneNumber phone,
		UserStatus status,
		Instant createdAt,
		Instant updatedAt) {

	private static final int MAX_NAME_LENGTH = 100;

	public User {
		id = Objects.requireNonNull(id, "id is required");
		email = Objects.requireNonNull(email, "email is required");
		firstName = requireName(firstName, "firstName");
		lastName = requireName(lastName, "lastName");
		phone = (phone == null) ? new PhoneNumber(null) : phone;
		status = Objects.requireNonNull(status, "status is required");
		createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
		updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
	}

	private static String requireName(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new InvalidUserException(field + " is required");
		}
		String normalized = value.trim();
		if (normalized.length() > MAX_NAME_LENGTH) {
			throw new InvalidUserException(field + " must have at most 100 characters");
		}
		return normalized;
	}

	public User update(Email newEmail, String newFirstName, String newLastName, PhoneNumber newPhone,
			UserStatus newStatus, Instant now) {
		return new User(id, newEmail, newFirstName, newLastName, newPhone, newStatus, createdAt, now);
	}
}
