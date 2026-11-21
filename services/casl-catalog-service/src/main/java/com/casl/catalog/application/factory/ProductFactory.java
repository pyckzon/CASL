package com.casl.catalog.application.factory;

import com.casl.catalog.application.command.CreateProductCommand;
import com.casl.catalog.application.command.UpdateProductCommand;
import com.casl.catalog.domain.exception.DuplicateSkuException;
import com.casl.catalog.domain.model.Money;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductStatus;
import com.casl.catalog.domain.model.Sku;
import com.casl.catalog.domain.port.ProductRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Factory de creación del agregado Product. Centraliza la normalización
 * (SKU, Money, status por defecto) para que los adaptadores no la dupliquen.
 */
public final class ProductFactory {

	private ProductFactory() {
	}

	public static Product create(CreateProductCommand command, ProductRepository repository, Clock clock) {
		Sku sku = Sku.of(command.sku());
		if (repository.existsBySku(sku)) {
			throw new DuplicateSkuException(sku.value());
		}
		Instant now = clock.instant();
		return new Product(
				UUID.randomUUID(),
				sku,
				command.name(),
				command.description(),
				Money.of(command.price(), command.currency()),
				command.categoryId(),
				ProductStatus.ACTIVE,
				Math.max(0, command.initialStock()),
				0,
				now,
				now);
	}

	public static Product applyUpdate(Product current, UpdateProductCommand command, Clock clock) {
		Money price = (command.price() == null) ? null
				: Money.of(command.price(), command.currency() == null ? current.price().currency().getCurrencyCode() : command.currency());
		return current.update(command.name(), command.description(), price, command.categoryId(), command.status(), clock.instant());
	}
}
