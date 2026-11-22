package com.casl.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Distingue tokens de usuario de tokens de servicio (client credentials) para
 * operaciones internas como cobrar un pedido.
 *
 * <p>En Keycloak el {@code sub} de una cuenta de servicio es un UUID, igual que
 * el de un usuario: la señal fiable es {@code preferred_username} con el prefijo
 * {@code service-account-}. Además se exige un {@code client_id} exacto para que
 * otra cuenta de servicio del realm no herede el permiso.
 */
public final class ServiceToken {

	private static final String SERVICE_ACCOUNT_PREFIX = "service-account-";

	private ServiceToken() {
	}

	public static boolean isService(Jwt jwt) {
		if (jwt == null) {
			return false;
		}
		String username = jwt.getClaimAsString("preferred_username");
		boolean byUsername = username != null && username.startsWith(SERVICE_ACCOUNT_PREFIX);
		boolean bySubject = jwt.getSubject() != null && jwt.getSubject().startsWith(SERVICE_ACCOUNT_PREFIX);
		return byUsername || bySubject;
	}

	/** Permite la operación a un ADMIN o a la cuenta de servicio indicada. */
	public static void requireManager(Jwt jwt, String allowedClientId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean admin = authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
		boolean service = isService(jwt) && allowedClientId.equals(clientId(jwt));
		if (!admin && !service) {
			throw new AccessDeniedException("Manager role or permitted service token required");
		}
	}

	private static String clientId(Jwt jwt) {
		String clientId = jwt.getClaimAsString("client_id");
		return clientId != null ? clientId : jwt.getClaimAsString("azp");
	}
}
