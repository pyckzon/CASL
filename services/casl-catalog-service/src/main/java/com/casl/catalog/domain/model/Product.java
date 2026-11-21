package com.casl.catalog.domain.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado de producto. La reserva de stock se delega a {@link com.casl.catalog.domain.port.StockPolicy};
 * el agregado expone la intenciÃ³n (reserve/release) y la polÃ­tica decide si es posible.
 */
public record Product(
		UUID id,
		Sku sku,
		String name,
		String description,
		Money price,
		UUID categoryId,
		ProductStatus status,
		int stockQuantity,
		int reservedQuantity,
		Instant createdAt,
		Instant updatedAt) implements java.io.Serializable {

	public Product {
		id = Objects.requireNonNull(id, "id is required");
		sku = Objects.requireNonNull(sku, "sku is required");
		if (name == null || name.isBlank()) {
			throw new com.casl.catalog.domain.exception.InvalidProductException("name is required");
		}
		name = name.trim();
		if (name.length() > 200) {
			throw new com.casl.catalog.domain.exception.InvalidProductException("name must have at most 200 characters");
		}
		price = Objects.requireNonNull(price, "price is required");
		status = Objects.requireNonNull(status, "status is required");
		if (stockQuantity < 0 || reservedQuantity < 0) {
			throw new com.casl.catalog.domain.exception.InvalidProductException("stock quantities must not be negative");
		}
		if (reservedQuantity > stockQuantity) {
			throw new com.casl.catalog.domain.exception.InvalidProductException("reserved quantity cannot exceed stock");
		}
		createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
		updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
	}

	public int availableQuantity() {
		return stockQuantity - reservedQuantity;
	}

	public Product reserve(int quantity, com.casl.catalog.domain.port.StockPolicy policy, java.time.Instant now) {
		if (!policy.canReserve(this, quantity)) {
			throw new com.casl.catalog.domain.exception.InsufficientStockException(sku.value(), quantity, availableQuantity());
		}
		return new Product(id, sku, name, description, price, categoryId, status, stockQuantity, reservedQuantity + quantity,
				createdAt, now);
	}

	public Product release(int quantity, java.time.Instant now) {
		int newReserved = Math.max(0, reservedQuantity - quantity);
		return new Product(id, sku, name, description, price, categoryId, status, stockQuantity, newReserved, createdAt, now);
	}

	public Product adjustStock(int newQuantity, java.time.Instant now) {
		if (newQuantity < reservedQuantity) {
			throw new com.casl.catalog.domain.exception.InvalidProductException(
					"stock cannot be below reserved quantity (" + reservedQuantity + ")");
		}
		return new Product(id, sku, name, description, price, categoryId, status, newQuantity, reservedQuantity, createdAt, now);
	}

	public Product update(String newName, String newDescription, Money newPrice, UUID newCategoryId, ProductStatus newStatus, java.time.Instant now) {
		return new Product(id, sku,
				newName == null || newName.isBlank() ? name : newName.trim(),
				newDescription == null ? description : newDescription.trim(),
				newPrice == null ? price : newPrice,
				newCategoryId == null ? categoryId : newCategoryId,
				newStatus == null ? status : newStatus,
				stockQuantity, reservedQuantity, createdAt, now);
	}
}
