package com.casl.catalog.application.command;

import java.util.UUID;

public record ReleaseStockCommand(UUID productId, int quantity) {
}
