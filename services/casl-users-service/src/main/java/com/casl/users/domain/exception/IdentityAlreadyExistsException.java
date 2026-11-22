package com.casl.users.domain.exception;

/**
 * La identidad ya está vinculada a un usuario. Puede ocurrir si dos peticiones
 * simultáneas intentan aprovisionar el mismo sujeto.
 */
public class IdentityAlreadyExistsException extends RuntimeException {

	public IdentityAlreadyExistsException(String subject) {
		super("Identity " + subject + " is already linked");
	}
}
