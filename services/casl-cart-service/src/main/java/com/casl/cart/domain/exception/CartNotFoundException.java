package com.casl.cart.domain.exception;

public class CartNotFoundException extends RuntimeException {
	public CartNotFoundException(java.util.UUID cartId) {
		super("No cart with id " + cartId);
	}
}
