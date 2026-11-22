package com.casl.order.adapter.in.web;

import com.casl.order.application.port.in.OrderUseCase;
import com.casl.order.config.SecurityConfig;
import com.casl.order.domain.exception.OrderNotFoundException;
import com.casl.order.domain.model.Order;
import com.casl.order.domain.model.OrderLine;
import com.casl.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = OrderController.class, excludeAutoConfiguration = {
		org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
		org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class OrderControllerTest {

	private static final String BASE = "/api/orders/v1/orders";
	private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORDER_ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000005");
	private static final UUID P1 = UUID.fromString("11111111-0000-0000-0000-000000000001");
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderUseCase useCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void createUsesAuthenticatedSubjectAsOwner() throws Exception {
		when(useCase.create(any())).thenReturn(order(OrderStatus.CREATED));

		mockMvc.perform(post(BASE).with(user(USER_ID))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"items":[{"productId":"11111111-0000-0000-0000-000000000001","sku":"SKU-1","name":"Widget","unitPrice":9.99,"currency":"USD","quantity":2}]}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
				.andExpect(jsonPath("$.userId").value(USER_ID.toString()))
				.andExpect(jsonPath("$.status").value("CREATED"));
	}

	@Test
	void getUnknownReturns404() throws Exception {
		when(useCase.getById(ORDER_ID)).thenThrow(new OrderNotFoundException(ORDER_ID));

		mockMvc.perform(get(BASE + "/{id}", ORDER_ID).with(user(USER_ID)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
	}

	@Test
	void payChecksOwnership() throws Exception {
		when(useCase.getById(ORDER_ID)).thenReturn(order(OrderStatus.PAYMENT_PENDING));
		when(useCase.processPayment(ORDER_ID)).thenReturn(order(OrderStatus.PAID));

		mockMvc.perform(post(BASE + "/{id}/pay", ORDER_ID).with(user(USER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAID"));
	}

	@Test
	void cancelChecksOwnership() throws Exception {
		when(useCase.getById(ORDER_ID)).thenReturn(order(OrderStatus.CREATED));
		when(useCase.cancel(any())).thenReturn(order(OrderStatus.CANCELLED));

		mockMvc.perform(post(BASE + "/{id}/cancel", ORDER_ID).with(user(USER_ID))
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	private static RequestPostProcessor user(UUID subject) {
		return jwt()
				.jwt(token -> token.subject(subject.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_USER"));
	}

	private static Order order(OrderStatus status) {
		return new Order(ORDER_ID, USER_ID,
				List.of(new OrderLine(P1, "SKU-1", "Widget", new BigDecimal("9.99"), "USD", 2)),
				status, new BigDecimal("19.98"), "USD", null, null, NOW, NOW);
	}
}
