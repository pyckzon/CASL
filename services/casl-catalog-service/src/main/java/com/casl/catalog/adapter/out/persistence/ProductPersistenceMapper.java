package com.casl.catalog.adapter.out.persistence;

import com.casl.catalog.domain.model.Money;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductStatus;
import com.casl.catalog.domain.model.Sku;

import java.util.Currency;

final class ProductPersistenceMapper {

	private ProductPersistenceMapper() {
	}

	static ProductJpaEntity toEntity(Product product) {
		return new ProductJpaEntity(
				product.id(),
				product.sku().value(),
				product.name(),
				product.description(),
				product.price().amount(),
				product.price().currency().getCurrencyCode(),
				product.categoryId(),
				product.status().name(),
				product.stockQuantity(),
				product.reservedQuantity(),
				product.createdAt(),
				product.updatedAt());
	}

	static Product toDomain(ProductJpaEntity entity) {
		return new Product(
				entity.getId(),
				Sku.of(entity.getSku()),
				entity.getName(),
				entity.getDescription(),
				new Money(entity.getPrice(), Currency.getInstance(entity.getCurrency())),
				entity.getCategoryId(),
				ProductStatus.valueOf(entity.getStatus()),
				entity.getStockQuantity(),
				entity.getReservedQuantity(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
