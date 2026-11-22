package com.casl.users.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Vínculo entre una identidad externa (por ejemplo Keycloak) y el usuario del
 * dominio. Se modela aparte del agregado {@link User} porque la autenticación
 * es un contexto distinto: el usuario no necesita conocer quién lo autentica.
 *
 * @param provider proveedor de identidad, por ejemplo {@link #KEYCLOAK}
 * @param subject  identificador del sujeto en el proveedor (claim {@code sub})
 */
public record UserIdentity(UUID userId, String provider, String subject) {

	public static final String KEYCLOAK = "keycloak";

	private static final int MAX_PROVIDER_LENGTH = 40;
	private static final int MAX_SUBJECT_LENGTH = 100;

	public UserIdentity {
		userId = Objects.requireNonNull(userId, "userId is required");
		provider = require(provider, "provider", MAX_PROVIDER_LENGTH);
		subject = require(subject, "subject", MAX_SUBJECT_LENGTH);
	}

	private static String require(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required");
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(field + " is too long");
		}
		return normalized;
	}
}
