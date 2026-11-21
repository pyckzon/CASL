package com.casl.cart.application.command;

import java.util.UUID;

public record MergeCartsCommand(UUID targetCartId, UUID sourceCartId) {
}
