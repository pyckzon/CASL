package com.casl.order.adapter.out.persistence;

import com.casl.order.domain.model.Order;
import com.casl.order.domain.port.OrderRepository;

import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public class OrderPersistenceAdapter implements OrderRepository {

	private final OrderJpaRepository repository;

	public OrderPersistenceAdapter(OrderJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Order save(Order order) {
		OrderJpaEntity entity = new OrderJpaEntity(
				order.orderId(), order.userId(), order.status(), order.totalAmount(), order.currency(),
				order.paymentId(), order.failureReason(), order.lines(), order.createdAt(), order.updatedAt());
		OrderJpaEntity saved = repository.saveAndFlush(entity);
		return toDomain(saved);
	}

	@Override
	public Optional<Order> findById(UUID orderId) {
		return repository.findById(orderId).map(this::toDomain);
	}

	private Order toDomain(OrderJpaEntity e) {
		return new Order(e.getId(), e.getUserId(), e.getLines(), e.getStatus(), e.getTotalAmount(), e.getCurrency(),
				e.getPaymentId(), e.getFailureReason(), e.getCreatedAt(), e.getUpdatedAt());
	}
}
