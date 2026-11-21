package com.casl.catalog.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Criterios de búsqueda combinables (Strategy). Todos opcionales.
 */
public record ProductSearchCriteria(
		String name,
		UUID categoryId,
		BigDecimal minPrice,
		BigDecimal maxPrice,
		ProductStatus status,
		int page,
		int size) {

	public ProductSearchCriteria {
		if (page < 0) {
			throw new IllegalArgumentException("page must not be negative");
		}
		if (size < 1 || size > 100) {
			throw new IllegalArgumentException("size must be between 1 and 100");
		}
	}
}
