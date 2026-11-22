package com.casl.order.domain.model;

public enum OrderStatus {
	CREATED,
	PAYMENT_PENDING,
	PAID,
	PAYMENT_FAILED,
	CANCELLED,
	SHIPPED,
	DELIVERED
}
