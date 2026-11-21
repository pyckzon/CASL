package com.casl.users.adapter.in.web;

import com.casl.users.domain.model.UserStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Representación pública de un usuario. No expone nada que no esté en el
 * contrato de la API.
 */
public record UserResponse(
		UUID id,
		String email,
		String firstName,
		String lastName,
		String phone,
		UserStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
