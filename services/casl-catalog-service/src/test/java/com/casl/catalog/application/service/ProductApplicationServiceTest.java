package com.casl.catalog.application.service;

import com.casl.catalog.application.command.AdjustStockCommand;
import com.casl.catalog.application.command.CreateProductCommand;
import com.casl.catalog.application.command.ReleaseStockCommand;
import com.casl.catalog.application.command.ReserveStockCommand;
import com.casl.catalog.application.command.UpdateProductCommand;
import com.casl.catalog.config.OptimisticStockPolicy;
import com.casl.catalog.domain.exception.DuplicateSkuException;
import com.casl.catalog.domain.exception.InsufficientStockException;
import com.casl.catalog.domain.exception.ProductNotFoundException;
import com.casl.catalog.domain.model.Money;
import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductSearchCriteria;
import com.casl.catalog.domain.model.ProductStatus;
import com.casl.catalog.domain.model.Sku;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductApplicationServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
	private static final Instant LATER = NOW.plusSeconds(3600);

	private final InMemoryProductRepository repository = new InMemoryProductRepository();
	private final InMemoryProductSearchStrategy strategy = new InMemoryProductSearchStrategy(repository);
	private final ProductApplicationService service = serviceAt(NOW);

	@Test
	void createNormalizesAndPersists() {
		Product product = service.create(new CreateProductCommand("sku-1", "Widget", "desc", new BigDecimal("9.99"),
				"usd", null, 10));

		assertThat(product.id()).isNotNull();
		assertThat(product.sku().value()).isEqualTo("SKU-1");
		assertThat(product.price().currency().getCurrencyCode()).isEqualTo("USD");
		assertThat(product.status()).isEqualTo(ProductStatus.ACTIVE);
		assertThat(product.stockQuantity()).isEqualTo(10);
		assertThat(repository.findById(product.id())).contains(product);
	}

	@Test
	void createRejectsDuplicateSku() {
		service.create(new CreateProductCommand("sku-1", "Widget", null, new BigDecimal("9.99"), "USD", null, 5));

		assertThatThrownBy(() -> service.create(new CreateProductCommand("sku-1", "Other", null, new BigDecimal("1.00"), "USD", null, 1)))
				.isInstanceOf(DuplicateSkuException.class);
	}

	@Test
	void getByIdReturnsOrThrows() {
		Product created = service.create(new CreateProductCommand("sku-1", "Widget", null, new BigDecimal("9.99"), "USD", null, 5));

		assertThat(service.getById(created.id())).isEqualTo(created);
		UUID unknown = UUID.randomUUID();
		assertThatThrownBy(() -> service.getById(unknown)).isInstanceOf(ProductNotFoundException.class);
	}

	@Test
	void searchFiltersByCriteria() {
		service.create(new CreateProductCommand("sku-1", "Widget", null, new BigDecimal("9.99"), "USD", null, 5));
		service.create(new CreateProductCommand("sku-2", "Gadget", null, new BigDecimal("49.99"), "USD", null, 3));

		PageResult<Product> all = service.search(new ProductSearchCriteria(null, null, null, null, null, 0, 20));
		PageResult<Product> byName = service.search(new ProductSearchCriteria("gadget", null, null, null, null, 0, 20));
		PageResult<Product> byPrice = service.search(
				new ProductSearchCriteria(null, null, new BigDecimal("10.00"), new BigDecimal("60.00"), null, 0, 20));

		assertThat(all.totalElements()).isEqualTo(2);
		assertThat(byName.content()).singleElement().extracting(Product::name).isEqualTo("Gadget");
		assertThat(byPrice.content()).singleElement().extracting(Product::name).isEqualTo("Gadget");
	}

	@Test
	void updateReplacesFields() {
		Product created = service.create(new CreateProductCommand("sku-1", "Widget", "old", new BigDecimal("9.99"), "USD", null, 5));

		Product updated = serviceAt(LATER).update(created.id(),
				new UpdateProductCommand("Widget Pro", "new desc", new BigDecimal("19.99"), "EUR", null, ProductStatus.INACTIVE));

		assertThat(updated.name()).isEqualTo("Widget Pro");
		assertThat(updated.description()).isEqualTo("new desc");
		assertThat(updated.price().amount()).isEqualByComparingTo("19.99");
		assertThat(updated.price().currency().getCurrencyCode()).isEqualTo("EUR");
		assertThat(updated.status()).isEqualTo(ProductStatus.INACTIVE);
		assertThat(updated.updatedAt()).isEqualTo(LATER);
	}

	@Test
	void deleteRemovesProduct() {
		Product created = service.create(new CreateProductCommand("sku-1", "Widget", null, new BigDecimal("9.99"), "USD", null, 5));

		service.delete(created.id());

		assertThatThrownBy(() -> service.getById(created.id())).isInstanceOf(ProductNotFoundException.class);
	}

	@Test
	void adjustStockUpdatesQuantity() {
		Product created = service.create(new CreateProductCommand("sku-1", "Widget", null, new BigDecimal("9.99"), "USD", null, 5));

		Product adjusted = service.adjustStock(new AdjustStockCommand(created.id(), 20));

		assertThat(adjusted.stockQuantity()).isEqualTo(20);
	}

	@Test
	void reserveAndReleaseStock() {
		Product created = service.create(new CreateProductCommand("sku-1", "Widget", null, new BigDecimal("9.99"), "USD", null, 5));

		Product reserved = service.reserve(new ReserveStockCommand(created.id(), 3));
		assertThat(reserved.reservedQuantity()).isEqualTo(3);
		assertThat(reserved.availableQuantity()).isEqualTo(2);

		Product released = service.release(new ReleaseStockCommand(created.id(), 2));
		assertThat(released.reservedQuantity()).isEqualTo(1);
	}

	@Test
	void reserveThrowsWhenInsufficient() {
		Product created = service.create(new CreateProductCommand("sku-1", "Widget", null, new BigDecimal("9.99"), "USD", null, 2));

		assertThatThrownBy(() -> service.reserve(new ReserveStockCommand(created.id(), 5)))
				.isInstanceOf(InsufficientStockException.class);
	}

	@Test
	void getBySku() {
		Product created = service.create(new CreateProductCommand("sku-1", "Widget", null, new BigDecimal("9.99"), "USD", null, 5));

		assertThat(service.getBySku("sku-1")).isEqualTo(created);
		assertThatThrownBy(() -> service.getBySku("nope")).isInstanceOf(ProductNotFoundException.class);
	}

	private ProductApplicationService serviceAt(Instant instant) {
		return new ProductApplicationService(repository, strategy, new OptimisticStockPolicy(),
				Clock.fixed(instant, ZoneOffset.UTC));
	}
}
