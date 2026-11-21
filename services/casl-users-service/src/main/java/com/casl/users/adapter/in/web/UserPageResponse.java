package com.casl.users.adapter.in.web;

import java.util.List;

/**
 * Página de usuarios según el contrato: metadatos explícitos y sin estructuras
 * propias de Spring Data.
 */
public record UserPageResponse(
		List<UserResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
