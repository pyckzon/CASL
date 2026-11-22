package com.casl.order.application.port.in;

import com.casl.order.application.command.CancelOrderCommand;
import com.casl.order.application.command.CreateOrderCommand;
import com.casl.order.domain.model.Order;

import java.util.UUID;

public interface OrderUseCase {
	Order create(CreateOrderCommand command);
	Order getById(UUID orderId);
	Order processPayment(UUID orderId);
	Order cancel(CancelOrderCommand command);
}
