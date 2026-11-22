package com.casl.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Anti-corruption de identidad para servicios que no gestionan usuarios: el
 * identificador del recurso se deriva del {@code sub} validado del token y
 * nunca de un parámetro de la petición ni de un header propagado.
 *
 * <p>Keycloak emite sujetos UUID; si un proveedor emitiera otro formato, el
 * token se rechaza en lugar de traducirlo de forma ambigua.
 */
public final class JwtSubject {

	private JwtSubject() {
	}

	public static UUID requireUuid(Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
			throw new AuthenticationCredentialsNotFoundException("Token subject is required");
		}
		try {
			return UUID.fromString(jwt.getSubject());
		}
		catch (IllegalArgumentException exception) {
			throw new BadCredentialsException("Token subject is not a UUID", exception);
		}
	}

	public static void requireOwner(UUID ownerId, Jwt jwt) {
		if (!requireUuid(jwt).equals(ownerId)) {
			throw new AccessDeniedException("The resource belongs to another user");
		}
	}
}
