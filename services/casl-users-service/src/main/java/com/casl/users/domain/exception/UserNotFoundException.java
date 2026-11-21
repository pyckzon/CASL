package com.casl.users.domain.exception;

import java.util.UUID;

/**
 * El usuario solicitado no existe. Es un error de negocio, no técnico.
 */
public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException(UUID id) {
		super("No user with id " + id);
	}
}
