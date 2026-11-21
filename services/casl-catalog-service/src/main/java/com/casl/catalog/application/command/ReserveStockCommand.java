package com.casl.catalog.application.command;

import java.util.UUID;

public record ReserveStockCommand(UUID productId, int quantity) {
}
