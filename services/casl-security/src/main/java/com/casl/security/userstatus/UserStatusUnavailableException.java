package com.casl.security.userstatus;

/**
 * El estado del usuario no pudo verificarse (timeout, indisponibilidad o error
 * del servicio de usuarios). Fail-closed: sin valor verificado no hay acceso.
 */
public class UserStatusUnavailableException extends RuntimeException {

	public UserStatusUnavailableException(String message) {
		super(message);
	}

	public UserStatusUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
