package com.casl.catalog.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 64, unique = true)
	private String sku;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal price;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(name = "category_id")
	private UUID categoryId;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(name = "stock_quantity", nullable = false)
	private int stockQuantity;

	@Column(name = "reserved_quantity", nullable = false)
	private int reservedQuantity;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ProductJpaEntity() {
	}

	public ProductJpaEntity(UUID id, String sku, String name, String description, BigDecimal price, String currency,
			UUID categoryId, String status, int stockQuantity, int reservedQuantity, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.sku = sku;
		this.name = name;
		this.description = description;
		this.price = price;
		this.currency = currency;
		this.categoryId = categoryId;
		this.status = status;
		this.stockQuantity = stockQuantity;
		this.reservedQuantity = reservedQuantity;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public UUID getId() { return id; }
	public String getSku() { return sku; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public BigDecimal getPrice() { return price; }
	public String getCurrency() { return currency; }
	public UUID getCategoryId() { return categoryId; }
	public String getStatus() { return status; }
	public int getStockQuantity() { return stockQuantity; }
	public int getReservedQuantity() { return reservedQuantity; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
