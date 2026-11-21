package com.casl.catalog.adapter.in.web;

import java.util.List;

public record ProductPageResponse(
		List<ProductResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {
}
