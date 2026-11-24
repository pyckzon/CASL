package com.casl.security.userstatus;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Contrato de verificación de estado del usuario. Lo implementa el servicio
 * dueño del dato ({@code users}) o, en los demás desplegables, el cliente remoto
 * que reenvía el JWT del llamador.
 */
public interface UserStatusVerifier {

	/**
	 * Devuelve el estado del usuario identificado por el subject del token.
	 *
	 * @throws UserStatusUnavailableException si no se pudo verificar el estado.
	 */
	UserStatus verify(Jwt jwt);
}
