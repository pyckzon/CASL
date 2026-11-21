package com.casl.catalog.domain.exception;

public class DuplicateSkuException extends RuntimeException {
	public DuplicateSkuException(String sku) {
		super("The sku " + sku + " is already registered");
	}
}
