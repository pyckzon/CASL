package com.casl.catalog.application.command;

import com.casl.catalog.domain.model.ProductStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductCommand(
		String name,
		String description,
		BigDecimal price,
		String currency,
		UUID categoryId,
		ProductStatus status) {
}
