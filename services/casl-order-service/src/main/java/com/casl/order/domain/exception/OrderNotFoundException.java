package com.casl.order.domain.exception;

public class OrderNotFoundException extends RuntimeException {
	public OrderNotFoundException(java.util.UUID id) {
		super("No order with id " + id);
	}
}
