package com.casl.cart.adapter.in.web;

import jakarta.validation.constraints.Min;

public record UpdateQuantityRequest(@Min(0) int quantity) {
}
