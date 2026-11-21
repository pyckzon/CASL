package com.casl.cart.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CartTest {

	private static final UUID CART_ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000003");
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
	private static final UUID P1 = UUID.fromString("11111111-0000-0000-0000-000000000001");
	private static final UUID P2 = UUID.fromString("22222222-0000-0000-0000-000000000002");

	private static CartItem item(UUID productId, int qty, String price) {
		return new CartItem(productId, "SKU-" + productId.toString().substring(0, 4), "Product", new BigDecimal(price), "USD", qty);
	}

	@Test
	void addItemIncrementsExistingSku() {
		Cart cart = Cart.empty(CART_ID, null, NOW).addItem(item(P1, 2, "9.99"));

		Cart updated = cart.addItem(item(P1, 3, "9.99"));

		assertThat(updated.items()).hasSize(1);
		assertThat(updated.items().get(0).quantity()).isEqualTo(5);
	}

	@Test
	void addItemAppendsNewProduct() {
		Cart cart = Cart.empty(CART_ID, null, NOW).addItem(item(P1, 2, "9.99"));

		Cart updated = cart.addItem(item(P2, 1, "4.50"));

		assertThat(updated.items()).hasSize(2);
		assertThat(updated.totalItems()).isEqualTo(3);
	}

	@Test
	void updateQuantityZeroRemovesItem() {
		Cart cart = Cart.empty(CART_ID, null, NOW).addItem(item(P1, 2, "9.99"));

		Cart updated = cart.updateQuantity(P1, 0);

		assertThat(updated.items()).isEmpty();
	}

	@Test
	void removeItemIsIdempotent() {
		Cart cart = Cart.empty(CART_ID, null, NOW).addItem(item(P1, 2, "9.99"));

		Cart updated = cart.removeItem(P2);

		assertThat(updated.items()).hasSize(1);
	}

	@Test
	void clearEmptiesCart() {
		Cart cart = Cart.empty(CART_ID, null, NOW).addItem(item(P1, 2, "9.99"));

		assertThat(cart.clear().items()).isEmpty();
	}

	@Test
	void mergeSumsQuantities() {
		Cart a = Cart.empty(CART_ID, null, NOW).addItem(item(P1, 2, "9.99"));
		Cart b = Cart.empty(UUID.randomUUID(), null, NOW).addItem(item(P1, 3, "9.99")).addItem(item(P2, 1, "4.50"));

		Cart merged = a.merge(b);

		assertThat(merged.items()).hasSize(2);
		assertThat(merged.items().stream().filter(i -> i.productId().equals(P1)).findFirst().get().quantity()).isEqualTo(5);
	}

	@Test
	void totalAmountSumsLineTotals() {
		Cart cart = Cart.empty(CART_ID, null, NOW)
				.addItem(item(P1, 2, "9.99"))
				.addItem(item(P2, 1, "4.50"));

		assertThat(cart.totalAmount()).isEqualByComparingTo("24.48");
	}
}
