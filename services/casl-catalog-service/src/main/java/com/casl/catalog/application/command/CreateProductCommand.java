package com.casl.catalog.application.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductCommand(
		String sku,
		String name,
		String description,
		BigDecimal price,
		String currency,
		UUID categoryId,
		int initialStock) {
}
