package com.casl.users.domain.exception;

import java.util.UUID;

/**
 * El usuario existe pero está inactivo: no puede operar aunque su identidad
 * siga válida en el proveedor.
 */
public class InactiveUserException extends RuntimeException {

	public InactiveUserException(UUID id) {
		super("User with id " + id + " is inactive");
	}
}
