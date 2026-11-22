package com.casl.order.application.service;

import com.casl.order.application.command.CreateOrderCommand;
import com.casl.order.domain.exception.OrderNotFoundException;
import com.casl.order.domain.model.Order;
import com.casl.order.domain.model.OrderLine;
import com.casl.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderApplicationServiceTest {

	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
	private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
	private static final UUID P1 = UUID.fromString("11111111-0000-0000-0000-000000000001");

	private final InMemoryOrderRepository repository = new InMemoryOrderRepository();
	private final FakePaymentGateway gateway = new FakePaymentGateway();
	private final OrderApplicationService service = new OrderApplicationService(repository, gateway,
			Clock.fixed(NOW, ZoneOffset.UTC));

	private OrderLine line() {
		return new OrderLine(P1, "SKU-1", "Widget", new BigDecimal("9.99"), "USD", 2);
	}

	@Test
	void createPersistsOrderInCreatedState() {
		Order order = service.create(new CreateOrderCommand(USER_ID, List.of(line())));

		assertThat(order.orderId()).isNotNull();
		assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
		assertThat(order.totalAmount()).isEqualByComparingTo("19.98");
		assertThat(order.currency()).isEqualTo("USD");
		assertThat(repository.findById(order.orderId())).contains(order);
	}

	@Test
	void getByIdReturnsOrThrows() {
		Order created = service.create(new CreateOrderCommand(USER_ID, List.of(line())));

		assertThat(service.getById(created.orderId())).isEqualTo(created);
		UUID unknown = UUID.randomUUID();
		assertThatThrownBy(() -> service.getById(unknown)).isInstanceOf(OrderNotFoundException.class);
	}

	@Test
	void processPaymentApprovesAndMarksPaid() {
		Order created = service.create(new CreateOrderCommand(USER_ID, List.of(line())));

		Order paid = service.processPayment(created.orderId());

		assertThat(paid.status()).isEqualTo(OrderStatus.PAID);
		assertThat(paid.paymentId()).isNotNull();
	}

	@Test
	void processPaymentRejectsAndCancels() {
		gateway.setApprove(false);
		gateway.setFailureReason("insufficient funds");
		Order created = service.create(new CreateOrderCommand(USER_ID, List.of(line())));

		Order failed = service.processPayment(created.orderId());

		assertThat(failed.status()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(failed.failureReason()).contains("insufficient funds");
	}

	@Test
	void processPaymentIsIdempotentForPaidOrders() {
		Order created = service.create(new CreateOrderCommand(USER_ID, List.of(line())));
		Order paid = service.processPayment(created.orderId());

		Order again = service.processPayment(created.orderId());

		assertThat(again).isEqualTo(paid);
		assertThat(again.status()).isEqualTo(OrderStatus.PAID);
	}

	@Test
	void cancelMarksOrderCancelled() {
		Order created = service.create(new CreateOrderCommand(USER_ID, List.of(line())));

		Order cancelled = service.cancel(new com.casl.order.application.command.CancelOrderCommand(created.orderId(), "user request"));

		assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(cancelled.failureReason()).isEqualTo("user request");
	}

	@Test
	void processPaymentOnCancelledThrows() {
		Order created = service.create(new CreateOrderCommand(USER_ID, List.of(line())));
		service.cancel(new com.casl.order.application.command.CancelOrderCommand(created.orderId(), "x"));

		assertThatThrownBy(() -> service.processPayment(created.orderId()))
				.isInstanceOf(IllegalStateException.class);
	}
}
