package com.casl.users.domain.model;

/**
 * Estado de un usuario. Persistido como texto para que el valor del contrato
 * no dependa del orden del enum.
 */
public enum UserStatus {
	ACTIVE,
	INACTIVE
}
