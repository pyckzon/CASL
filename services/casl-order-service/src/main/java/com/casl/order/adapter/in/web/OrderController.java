package com.casl.order.adapter.in.web;

import com.casl.order.application.command.CancelOrderCommand;
import com.casl.order.application.command.CreateOrderCommand;
import com.casl.order.application.port.in.OrderUseCase;
import com.casl.order.domain.model.Order;
import com.casl.order.domain.model.OrderLine;
import com.casl.security.JwtSubject;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class OrderController {

	private static final String BASE = "/api/orders/v1/orders/";

	private final OrderUseCase useCase;

	public OrderController(OrderUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
			@AuthenticationPrincipal Jwt jwt) {
		List<OrderLine> lines = request.items().stream()
				.map(i -> new OrderLine(i.productId(), i.sku(), i.name(), i.unitPrice(), i.currency(), i.quantity()))
				.collect(Collectors.toList());
		Order created = useCase.create(new CreateOrderCommand(JwtSubject.requireUuid(jwt), lines));
		return ResponseEntity.created(URI.create(BASE + created.orderId()))
				.body(toResponse(created));
	}

	@GetMapping("/{orderId}")
	public OrderResponse get(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
		return toResponse(owned(orderId, jwt));
	}

	@PostMapping("/{orderId}/pay")
	public OrderResponse pay(@PathVariable UUID orderId, @AuthenticationPrincipal Jwt jwt) {
		owned(orderId, jwt);
		return toResponse(useCase.processPayment(orderId));
	}

	@PostMapping("/{orderId}/cancel")
	public OrderResponse cancel(@PathVariable UUID orderId, @RequestBody(required = false) CancelRequest body,
			@AuthenticationPrincipal Jwt jwt) {
		owned(orderId, jwt);
		String reason = (body == null) ? null : body.reason();
		return toResponse(useCase.cancel(new CancelOrderCommand(orderId, reason)));
	}

	private Order owned(UUID orderId, Jwt jwt) {
		Order order = useCase.getById(orderId);
		JwtSubject.requireOwner(order.userId(), jwt);
		return order;
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
