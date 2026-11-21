package com.casl.users.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/users/v1/users}. Solo valida forma; las reglas de
 * negocio viven en el dominio.
 */
public record CreateUserRequest(
		@NotBlank @Email @Size(max = 320) String email,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@Size(max = 30) String phone) {
}
