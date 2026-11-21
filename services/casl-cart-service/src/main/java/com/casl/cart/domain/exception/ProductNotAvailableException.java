package com.casl.cart.domain.exception;

public class ProductNotAvailableException extends RuntimeException {
	public ProductNotAvailableException(String sku) {
		super("Product not available: " + sku);
	}
}
