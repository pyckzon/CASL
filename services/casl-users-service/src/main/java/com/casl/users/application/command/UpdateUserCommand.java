package com.casl.users.application.command;

/**
 * Datos de entrada para reemplazar un usuario (PUT: reemplazo completo de los
 * campos mutables).
 */
public record UpdateUserCommand(
		String email,
		String firstName,
		String lastName,
		String phone,
		String status) {
}
