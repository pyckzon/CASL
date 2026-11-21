package com.casl.cart.adapter.in.web;

import com.casl.cart.application.command.AddItemCommand;
import com.casl.cart.application.command.MergeCartsCommand;
import com.casl.cart.application.command.RemoveItemCommand;
import com.casl.cart.application.command.UpdateQuantityCommand;
import com.casl.cart.domain.model.Cart;
import com.casl.cart.domain.model.CartItem;

import java.util.List;
import java.util.UUID;

final class CartWebMapper {

	private CartWebMapper() {
	}

	static AddItemCommand toAddCommand(UUID cartId, AddItemRequest request) {
		return new AddItemCommand(cartId, request.productId(), request.sku(), request.quantity());
	}

	static UpdateQuantityCommand toUpdateCommand(UUID cartId, UUID productId, UpdateQuantityRequest request) {
		return new UpdateQuantityCommand(cartId, productId, request.quantity());
	}

	static CartResponse toResponse(Cart cart) {
		List<CartItemResponse> items = cart.items().stream()
				.map(i -> new CartItemResponse(i.productId(), i.sku(), i.name(), i.unitPrice(), i.currency(),
						i.quantity(), i.lineTotal()))
				.toList();
		return new CartResponse(cart.cartId(), cart.userId(), items, cart.totalItems(), cart.totalAmount(),
				cart.createdAt(), cart.updatedAt());
	}
}
