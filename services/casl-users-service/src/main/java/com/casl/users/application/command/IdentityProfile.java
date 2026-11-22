package com.casl.users.application.command;

import com.casl.users.domain.model.UserIdentity;

/**
 * Datos de la identidad autenticada que llegan desde el token. Es un comando
 * de aplicación: el dominio no conoce JWT ni Keycloak.
 *
 * @param emailVerified indica si el proveedor verificó el email; solo entonces
 *                      se permite enlazar la identidad con un usuario existente
 */
public record IdentityProfile(
		String provider,
		String subject,
		String email,
		boolean emailVerified,
		String firstName,
		String lastName) {

	public IdentityProfile {
		provider = (provider == null || provider.isBlank()) ? UserIdentity.KEYCLOAK : provider;
	}
}
