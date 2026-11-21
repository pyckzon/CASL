package com.casl.cart.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado de carrito. Inmutable: cada operación devuelve un carrito nuevo.
 */
public record Cart(
		UUID cartId,
		UUID userId,
		List<CartItem> items,
		Instant createdAt,
		Instant updatedAt) {

	public Cart {
		cartId = Objects.requireNonNull(cartId, "cartId is required");
		items = List.copyOf(items);
		createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
		updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
	}

	public static Cart empty(UUID cartId, UUID userId, Instant now) {
		return new Cart(cartId, userId, List.of(), now, now);
	}

	public int totalItems() {
		return items.stream().mapToInt(CartItem::quantity).sum();
	}

	public BigDecimal totalAmount() {
		return items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public Cart addItem(CartItem newItem) {
		List<CartItem> updated = new ArrayList<>(items);
		int idx = -1;
		for (int i = 0; i < updated.size(); i++) {
			if (updated.get(i).productId().equals(newItem.productId())) {
				idx = i;
				break;
			}
		}
		if (idx >= 0) {
			CartItem existing = updated.get(idx);
			updated.set(idx, existing.withQuantity(existing.quantity() + newItem.quantity()));
		} else {
			updated.add(newItem);
		}
		return new Cart(cartId, userId, updated, createdAt, Instant.now());
	}

	public Cart updateQuantity(UUID productId, int quantity) {
		if (quantity < 1) {
			return removeItem(productId);
		}
		List<CartItem> updated = new ArrayList<>();
		for (CartItem item : items) {
			if (item.productId().equals(productId)) {
				updated.add(item.withQuantity(quantity));
			} else {
				updated.add(item);
			}
		}
		return new Cart(cartId, userId, updated, createdAt, Instant.now());
	}

	public Cart removeItem(UUID productId) {
		List<CartItem> updated = items.stream()
				.filter(i -> !i.productId().equals(productId))
				.toList();
		return new Cart(cartId, userId, updated, createdAt, Instant.now());
	}

	public Cart clear() {
		return new Cart(cartId, userId, List.of(), createdAt, Instant.now());
	}

	public Cart merge(Cart other) {
		Cart result = this;
		for (CartItem item : other.items()) {
			result = result.addItem(item);
		}
		return result;
	}
}
