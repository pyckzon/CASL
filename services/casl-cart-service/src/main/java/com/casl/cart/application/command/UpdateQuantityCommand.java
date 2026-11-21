package com.casl.cart.application.command;

import java.util.UUID;

public record UpdateQuantityCommand(UUID cartId, UUID productId, int quantity) {
}
