package com.casl.cart.application.command;

import java.util.UUID;

public record RemoveItemCommand(UUID cartId, UUID productId) {
}
