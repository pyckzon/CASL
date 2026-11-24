package com.casl.security.userstatus;

/**
 * Estado del usuario en la fuente de verdad (`users`). Se comprueba en todos
 * los endpoints protegidos a través del {@code UserStatusEnforcementFilter}.
 */
public enum UserStatus {
	ACTIVE,
	INACTIVE
}
