package com.casl.order.application.command;

import java.util.UUID;

public record CancelOrderCommand(UUID orderId, String reason) {
}
