package com.casl.order.adapter.in.web;

import com.casl.order.application.port.in.OrderUseCase;
import com.casl.order.domain.exception.OrderNotFoundException;
import com.casl.order.domain.model.Order;
import com.casl.order.domain.model.OrderLine;
import com.casl.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	private static final String BASE = "/api/orders/v1/orders";
	private static final UUID ORDER_ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000005");
	private static final UUID P1 = UUID.fromString("11111111-0000-0000-0000-000000000001");
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderUseCase useCase;

	private static Order order(OrderStatus status) {
		return new Order(ORDER_ID, null, List.of(new OrderLine(P1, "SKU-1", "Widget", new BigDecimal("9.99"), "USD", 2)),
				status, new BigDecimal("19.98"), "USD", null, null, NOW, NOW);
	}

	@Test
	void createReturns201() throws Exception {
		when(useCase.create(any())).thenReturn(order(com.casl.order.domain.model.OrderStatus.CREATED));

		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
				{"items":[{"productId":"11111111-0000-0000-0000-000000000001","sku":"SKU-1","name":"Widget","unitPrice":9.99,"currency":"USD","quantity":2}]}
				"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
				.andExpect(jsonPath("$.status").value("CREATED"))
				.andExpect(jsonPath("$.totalAmount").value(19.98));
	}

	@Test
	void getUnknownReturns404() throws Exception {
		when(useCase.getById(ORDER_ID)).thenThrow(new OrderNotFoundException(ORDER_ID));

		mockMvc.perform(get(BASE + "/{id}", ORDER_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
	}

	@Test
	void payReturnsPaid() throws Exception {
		when(useCase.processPayment(ORDER_ID)).thenReturn(order(com.casl.order.domain.model.OrderStatus.PAID));

		mockMvc.perform(post(BASE + "/{id}/pay", ORDER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAID"));
	}

	@Test
	void cancelReturnsCancelled() throws Exception {
		when(useCase.cancel(any())).thenReturn(order(com.casl.order.domain.model.OrderStatus.CANCELLED));

		mockMvc.perform(post(BASE + "/{id}/cancel", ORDER_ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}
}
