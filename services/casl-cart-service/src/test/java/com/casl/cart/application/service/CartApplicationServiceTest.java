package com.casl.cart.application.service;

import com.casl.cart.application.command.AddItemCommand;
import com.casl.cart.application.command.MergeCartsCommand;
import com.casl.cart.application.command.RemoveItemCommand;
import com.casl.cart.application.command.UpdateQuantityCommand;
import com.casl.cart.domain.exception.CartNotFoundException;
import com.casl.cart.domain.exception.ProductNotAvailableException;
import com.casl.cart.domain.model.Cart;
import com.casl.cart.domain.model.CartItem;
import com.casl.cart.domain.model.CatalogProduct;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartApplicationServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
	private static final UUID P1 = UUID.fromString("11111111-0000-0000-0000-000000000001");

	private final InMemoryCartRepository repository = new InMemoryCartRepository();
	private final FakeCatalogClient catalog = new FakeCatalogClient();
	private final CartApplicationService service = new CartApplicationService(repository, catalog,
			Clock.fixed(NOW, ZoneOffset.UTC));

	private CatalogProduct availableProduct() {
		CatalogProduct p = new CatalogProduct(P1, "SKU-1", "Widget", new BigDecimal("9.99"), "USD", 10, "ACTIVE");
		catalog.add(p);
		return p;
	}

	@Test
	void createAndGetCart() {
		Cart created = service.create(null);

		assertThat(created.cartId()).isNotNull();
		assertThat(service.get(created.cartId())).isEqualTo(created);
	}

	@Test
	void getUnknownThrows() {
		assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(CartNotFoundException.class);
	}

	@Test
	void addItemUsesCatalogPrice() {
		availableProduct();
		Cart cart = service.create(null);

		Cart updated = service.addItem(new AddItemCommand(cart.cartId(), P1, null, 2));

		assertThat(updated.items()).hasSize(1);
		CartItem item = updated.items().get(0);
		assertThat(item.productId()).isEqualTo(P1);
		assertThat(item.sku()).isEqualTo("SKU-1");
		assertThat(item.unitPrice()).isEqualByComparingTo("9.99");
		assertThat(item.quantity()).isEqualTo(2);
	}

	@Test
	void addItemBySku() {
		availableProduct();
		Cart cart = service.create(null);

		Cart updated = service.addItem(new AddItemCommand(cart.cartId(), null, "sku-1", 1));

		assertThat(updated.items()).hasSize(1);
	}

	@Test
	void addItemRejectsUnavailableProduct() {
		CatalogProduct p = new CatalogProduct(P1, "SKU-1", "Widget", new BigDecimal("9.99"), "USD", 0, "ACTIVE");
		catalog.add(p);
		Cart cart = service.create(null);

		assertThatThrownBy(() -> service.addItem(new AddItemCommand(cart.cartId(), P1, null, 1)))
				.isInstanceOf(ProductNotAvailableException.class);
	}

	@Test
	void addItemRejectsUnknownProduct() {
		Cart cart = service.create(null);

		assertThatThrownBy(() -> service.addItem(new AddItemCommand(cart.cartId(), P1, null, 1)))
				.isInstanceOf(ProductNotAvailableException.class);
	}

	@Test
	void updateQuantityAndRemove() {
		availableProduct();
		Cart cart = service.create(null);
		service.addItem(new AddItemCommand(cart.cartId(), P1, null, 2));

		Cart updated = service.updateQuantity(new UpdateQuantityCommand(cart.cartId(), P1, 5));
		assertThat(updated.items().get(0).quantity()).isEqualTo(5);

		Cart removed = service.removeItem(new com.casl.cart.application.command.RemoveItemCommand(cart.cartId(), P1));
		assertThat(removed.items()).isEmpty();
	}

	@Test
	void updateQuantityZeroRemoves() {
		availableProduct();
		Cart cart = service.create(null);
		service.addItem(new AddItemCommand(cart.cartId(), P1, null, 2));

		Cart updated = service.updateQuantity(new UpdateQuantityCommand(cart.cartId(), P1, 0));
		assertThat(updated.items()).isEmpty();
	}

	@Test
	void clearCart() {
		availableProduct();
		Cart cart = service.create(null);
		service.addItem(new AddItemCommand(cart.cartId(), P1, null, 2));

		Cart cleared = service.clear(cart.cartId());
		assertThat(cleared.items()).isEmpty();
	}

	@Test
	void mergeCarts() {
		availableProduct();
		Cart target = service.create(null);
		Cart source = service.create(null);
		service.addItem(new AddItemCommand(source.cartId(), P1, null, 3));

		Cart merged = service.merge(new MergeCartsCommand(target.cartId(), source.cartId()));

		assertThat(merged.items()).hasSize(1);
		assertThat(merged.items().get(0).quantity()).isEqualTo(3);
		assertThatThrownBy(() -> service.get(source.cartId())).isInstanceOf(CartNotFoundException.class);
	}
}
