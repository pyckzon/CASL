package com.casl.cart.application.command;

import java.util.UUID;

public record AddItemCommand(UUID cartId, UUID productId, String sku, int quantity) {
}
