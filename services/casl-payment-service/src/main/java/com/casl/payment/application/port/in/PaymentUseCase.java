package com.casl.payment.application.port.in;

import com.casl.payment.application.command.ChargeCommand;
import com.casl.payment.domain.model.Payment;

import java.util.UUID;

public interface PaymentUseCase {
	Payment charge(ChargeCommand command);
	Payment getById(UUID paymentId);
	Payment refund(UUID paymentId);
}
