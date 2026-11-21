package com.casl.cart.application.service;

import com.casl.cart.domain.model.Cart;
import com.casl.cart.domain.model.CatalogProduct;
import com.casl.cart.domain.port.CartRepository;
import com.casl.cart.domain.port.CatalogClient;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryCartRepository implements CartRepository {

	private final Map<UUID, Cart> carts = new LinkedHashMap<>();

	@Override
	public Cart save(Cart cart) {
		carts.put(cart.cartId(), cart);
		return cart;
	}

	@Override
	public Optional<Cart> findById(UUID cartId) {
		return Optional.ofNullable(carts.get(cartId));
	}

	@Override
	public void deleteById(UUID cartId) {
		carts.remove(cartId);
	}
}

class FakeCatalogClient implements CatalogClient {

	private final Map<UUID, CatalogProduct> products = new LinkedHashMap<>();

	void add(CatalogProduct product) {
		products.put(product.id(), product);
	}

	@Override
	public Optional<CatalogProduct> findById(UUID productId) {
		return Optional.ofNullable(products.get(productId));
	}

	@Override
	public Optional<CatalogProduct> findBySku(String sku) {
		return products.values().stream().filter(p -> p.sku().equalsIgnoreCase(sku)).findFirst();
	}
}
