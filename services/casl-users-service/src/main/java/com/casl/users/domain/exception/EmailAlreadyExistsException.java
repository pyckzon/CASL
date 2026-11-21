package com.casl.users.domain.exception;

/**
 * Ya existe un usuario con el email indicado (el email es único).
 */
public class EmailAlreadyExistsException extends RuntimeException {

	public EmailAlreadyExistsException(String email) {
		super("The email " + email + " is already registered");
	}
}
