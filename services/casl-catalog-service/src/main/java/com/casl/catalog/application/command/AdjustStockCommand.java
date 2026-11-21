package com.casl.catalog.application.command;

import java.util.UUID;

public record AdjustStockCommand(UUID productId, int newQuantity) {
}
