package com.casl.order.application.service;

import com.casl.order.domain.model.Order;
import com.casl.order.domain.port.OrderRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryOrderRepository implements OrderRepository {
	private final Map<UUID, Order> orders = new LinkedHashMap<>();

	@Override
	public Order save(Order order) {
		orders.put(order.orderId(), order);
		return order;
	}

	@Override
	public Optional<Order> findById(UUID orderId) {
		return Optional.ofNullable(orders.get(orderId));
	}
}
