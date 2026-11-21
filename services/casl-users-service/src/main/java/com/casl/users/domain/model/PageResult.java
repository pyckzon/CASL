package com.casl.users.domain.model;

import java.util.List;

/**
 * Página de resultados independiente del framework de persistencia, para que
 * el dominio no conozca {@code org.springframework.data.domain.Page}.
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

	public PageResult {
		content = List.copyOf(content);
		if (page < 0 || size < 1 || totalElements < 0) {
			throw new IllegalArgumentException("invalid page metadata");
		}
	}

	public int totalPages() {
		return (int) ((totalElements + size - 1) / size);
	}
}
