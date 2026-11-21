package com.casl.catalog.domain.model;

import com.casl.catalog.domain.exception.InsufficientStockException;
import com.casl.catalog.domain.exception.InvalidProductException;
import com.casl.catalog.domain.port.StockPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

	private static final UUID ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000002");
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
	private static final StockPolicy POLICY = (p, q) -> q > 0 && p.availableQuantity() >= q;

	private static Product product(int stock, int reserved) {
		return new Product(ID, Sku.of("SKU-1"), "Widget", "desc", Money.of(new BigDecimal("9.99"), "USD"), null,
				ProductStatus.ACTIVE, stock, reserved, NOW, NOW);
	}

	@Test
	void availableQuantitySubtractsReserved() {
		assertThat(product(10, 3).availableQuantity()).isEqualTo(7);
	}

	@Test
	void reserveIncreasesReservedWhenPolicyAllows() {
		Product reserved = product(10, 0).reserve(4, POLICY, NOW.plusSeconds(10));

		assertThat(reserved.reservedQuantity()).isEqualTo(4);
		assertThat(reserved.availableQuantity()).isEqualTo(6);
		assertThat(reserved.updatedAt()).isEqualTo(NOW.plusSeconds(10));
	}

	@Test
	void reserveThrowsWhenInsufficient() {
		assertThatThrownBy(() -> product(2, 0).reserve(5, POLICY, NOW))
				.isInstanceOf(InsufficientStockException.class);
	}

	@Test
	void releaseDecreasesReserved() {
		Product released = product(10, 4).release(2, NOW.plusSeconds(1));

		assertThat(released.reservedQuantity()).isEqualTo(2);
	}

	@Test
	void adjustStockRejectsBelowReserved() {
		assertThatThrownBy(() -> product(10, 4).adjustStock(2, NOW))
				.isInstanceOf(InvalidProductException.class);
	}

	@Test
	void adjustStockUpdatesQuantity() {
		assertThat(product(10, 4).adjustStock(20, NOW).stockQuantity()).isEqualTo(20);
	}

	@Test
	void updateKeepsIdentityAndSku() {
		Product updated = product(10, 0).update("New name", "new desc", Money.of(new BigDecimal("19.99"), "USD"), null,
				ProductStatus.INACTIVE, NOW.plusSeconds(5));

		assertThat(updated.id()).isEqualTo(ID);
		assertThat(updated.sku().value()).isEqualTo("SKU-1");
		assertThat(updated.name()).isEqualTo("New name");
		assertThat(updated.status()).isEqualTo(ProductStatus.INACTIVE);
		assertThat(updated.price().amount()).isEqualByComparingTo("19.99");
	}

	@Test
	void rejectsReservedAboveStock() {
		assertThatThrownBy(() -> product(2, 5))
				.isInstanceOf(InvalidProductException.class);
	}
}
