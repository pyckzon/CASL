package com.casl.catalog.adapter.in.web;

import com.casl.catalog.application.command.AdjustStockCommand;
import com.casl.catalog.application.command.CreateProductCommand;
import com.casl.catalog.application.command.UpdateProductCommand;
import com.casl.catalog.domain.model.Money;
import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductSearchCriteria;
import com.casl.catalog.domain.model.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

final class ProductWebMapper {

	private ProductWebMapper() {
	}

	static CreateProductCommand toCreateCommand(CreateProductRequest request) {
		return new CreateProductCommand(
				request.sku(),
				request.name(),
				request.description(),
				request.price(),
				request.currency(),
				request.categoryId(),
				request.initialStock() == null ? 0 : request.initialStock());
	}

	static UpdateProductCommand toUpdateCommand(UpdateProductRequest request) {
		return new UpdateProductCommand(
				request.name(),
				request.description(),
				request.price(),
				request.currency(),
				request.categoryId(),
				request.status());
	}

	static ProductResponse toResponse(Product product) {
		return new ProductResponse(
				product.id(),
				product.sku().value(),
				product.name(),
				product.description(),
				product.price().amount(),
				product.price().currency().getCurrencyCode(),
				product.categoryId(),
				product.status(),
				product.stockQuantity(),
				product.reservedQuantity(),
				product.availableQuantity(),
				product.createdAt(),
				product.updatedAt());
	}

	static ProductPageResponse toPageResponse(PageResult<Product> page) {
		List<ProductResponse> content = page.content().stream().map(ProductWebMapper::toResponse).toList();
		return new ProductPageResponse(content, page.page(), page.size(), page.totalElements(), page.totalPages());
	}

	static ProductSearchCriteria toCriteria(String name, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
			ProductStatus status, int page, int size) {
		return new ProductSearchCriteria(name, categoryId, minPrice, maxPrice, status, page, size);
	}
}
