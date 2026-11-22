package com.casl.order.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * El dueño del pedido NO viaja en el contrato: se deriva del JWT validado para
 * evitar que un cliente cree pedidos a nombre de otro usuario.
 */
public record CreateOrderRequest(
		@NotEmpty List<@Valid OrderLineRequest> items) {
}
