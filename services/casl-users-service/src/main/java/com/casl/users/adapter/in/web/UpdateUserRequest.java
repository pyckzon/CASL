package com.casl.users.adapter.in.web;

import com.casl.users.domain.model.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code PUT /api/users/v1/users/{id}}: reemplazo completo de los
 * campos mutables, incluido el estado.
 */
public record UpdateUserRequest(
		@NotBlank @Email @Size(max = 320) String email,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@Size(max = 30) String phone,
		@NotNull UserStatus status) {
}
