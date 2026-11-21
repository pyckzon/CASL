package com.casl.catalog.adapter.in.web;

import com.casl.catalog.domain.model.ProductStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
		@Size(max = 200) String name,
		@Size(max = 2000) String description,
		@DecimalMin("0.00") BigDecimal price,
		@Size(min = 3, max = 3) String currency,
		UUID categoryId,
		ProductStatus status) {
}
