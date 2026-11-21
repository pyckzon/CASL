package com.casl.users.domain.exception;

/**
 * Los datos de entrada no cumplen las invariantes del dominio.
 */
public class InvalidUserException extends RuntimeException {

	public InvalidUserException(String message) {
		super(message);
	}
}
