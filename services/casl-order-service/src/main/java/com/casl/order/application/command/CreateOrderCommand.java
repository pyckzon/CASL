package com.casl.order.application.command;

import com.casl.order.domain.model.OrderLine;

import java.util.List;
import java.util.UUID;

public record CreateOrderCommand(UUID userId, List<OrderLine> lines) {
}
