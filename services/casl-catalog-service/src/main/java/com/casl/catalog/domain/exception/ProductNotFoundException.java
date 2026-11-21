package com.casl.catalog.domain.exception;

public class ProductNotFoundException extends RuntimeException {
	public ProductNotFoundException(java.util.UUID id) {
		super("No product with id " + id);
	}
	public ProductNotFoundException(String sku) {
		super("No product with sku " + sku);
	}
}
