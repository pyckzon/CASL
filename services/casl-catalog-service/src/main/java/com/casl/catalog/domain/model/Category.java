package com.casl.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * CategorÃ­a de producto. Inmutable.
 */
public record Category(
		UUID id,
		String name,
		String description,
		Instant createdAt,
		Instant updatedAt) implements java.io.Serializable {

	public Category {
		id = Objects.requireNonNull(id, "id is required");
		if (name == null || name.isBlank()) {
			throw new com.casl.catalog.domain.exception.InvalidCategoryException("name is required");
		}
		name = name.trim();
		if (name.length() > 100) {
			throw new com.casl.catalog.domain.exception.InvalidCategoryException("name must have at most 100 characters");
		}
		createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
		updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
	}
}
