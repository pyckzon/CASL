package com.casl.order.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
		UUID userId,
		@NotEmpty List<@Valid OrderLineRequest> items) {
}
