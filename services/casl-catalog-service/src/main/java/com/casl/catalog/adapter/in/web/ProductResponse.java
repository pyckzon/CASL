package com.casl.catalog.adapter.in.web;

import com.casl.catalog.domain.model.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
		UUID id,
		String sku,
		String name,
		String description,
		BigDecimal price,
		String currency,
		UUID categoryId,
		ProductStatus status,
		int stockQuantity,
		int reservedQuantity,
		int availableQuantity,
		Instant createdAt,
		Instant updatedAt) {
}
