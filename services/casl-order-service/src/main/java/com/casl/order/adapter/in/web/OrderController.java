package com.casl.order.adapter.in.web;

import com.casl.order.application.command.CancelOrderCommand;
import com.casl.order.application.command.CreateOrderCommand;
import com.casl.order.application.port.in.OrderUseCase;
import com.casl.order.domain.model.Order;
import com.casl.order.domain.model.OrderLine;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders/v1/orders")
public class OrderController {

	private static final String BASE = "/api/orders/v1/orders/";

	private final OrderUseCase useCase;

	public OrderController(OrderUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
		List<OrderLine> lines = request.items().stream()
				.map(i -> new OrderLine(i.productId(), i.sku(), i.name(), i.unitPrice(), i.currency(), i.quantity()))
				.collect(Collectors.toList());
		Order created = useCase.create(new CreateOrderCommand(request.userId(), lines));
		return ResponseEntity.created(URI.create(BASE + created.orderId()))
				.body(toResponse(created));
	}

	@GetMapping("/{orderId}")
	public OrderResponse get(@PathVariable UUID orderId) {
		return toResponse(useCase.getById(orderId));
	}

	@PostMapping("/{orderId}/pay")
	public OrderResponse pay(@PathVariable UUID orderId) {
		return toResponse(useCase.processPayment(orderId));
	}

	@PostMapping("/{orderId}/cancel")
	public OrderResponse cancel(@PathVariable UUID orderId, @RequestBody(required = false) CancelRequest body) {
		String reason = (body == null) ? null : body.reason();
		return toResponse(useCase.cancel(new CancelOrderCommand(orderId, reason)));
	}

	private static OrderResponse toResponse(Order order) {
		List<OrderLineResponse> lines = order.lines().stream()
				.map(l -> new OrderLineResponse(l.productId(), l.sku(), l.name(), l.unitPrice(), l.currency(),
						l.quantity(), l.lineTotal()))
				.toList();
		return new OrderResponse(order.orderId(), order.userId(), order.status(), order.totalAmount(), order.currency(),
				order.paymentId(), order.failureReason(), lines, order.createdAt(), order.updatedAt());
	}

	public record CancelRequest(String reason) {
	}
}
