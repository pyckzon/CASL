package com.casl.cart.adapter.in.web;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MergeCartRequest(@NotNull UUID sourceCartId) {
}
