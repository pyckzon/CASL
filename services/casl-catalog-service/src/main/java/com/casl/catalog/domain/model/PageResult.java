package com.casl.catalog.domain.model;

import java.util.List;

public record PageResult<T>(List<T> content, int page, int size, long totalElements) implements java.io.Serializable {

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
