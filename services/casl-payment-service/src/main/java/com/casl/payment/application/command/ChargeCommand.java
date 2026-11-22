package com.casl.payment.application.command;

import java.math.BigDecimal;

public record ChargeCommand(BigDecimal amount, String currency, String idempotencyKey) {
}
