package com.casl.order.adapter.in.web;

import com.casl.order.application.command.CreateOrderCommand;
import com.casl.order.application.port.in.OrderUseCase;
import com.casl.order.config.SecurityConfig;
import com.casl.order.domain.model.Order;
import com.casl.order.domain.model.OrderLine;
import com.casl.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Zero-trust en pedidos: dueño derivado del token, nunca del body. */
@WebMvcTest(value = OrderController.class, excludeAutoConfiguration = {
		org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
		org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class OrderSecurityTest {

	private static final String BASE = "/api/orders/v1/orders";
	private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORDER_ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000005");
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
	private static final String VALID_BODY = """
			{"items":[{"productId":"11111111-0000-0000-0000-000000000001","sku":"SKU-1","name":"Widget","unitPrice":9.99,"currency":"USD","quantity":2}]}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderUseCase useCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void requestWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get(BASE + "/{id}", ORDER_ID))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		verifyNoInteractions(useCase);
	}

	@Test
	void userCannotReadAnotherUsersOrder() throws Exception {
		when(useCase.getById(ORDER_ID)).thenReturn(order());

		mockMvc.perform(get(BASE + "/{id}", ORDER_ID).with(user(UUID.randomUUID())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void ownerCanReadHisOrder() throws Exception {
		when(useCase.getById(ORDER_ID)).thenReturn(order());

		mockMvc.perform(get(BASE + "/{id}", ORDER_ID).with(user(OWNER)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(OWNER.toString()));
	}

	@Test
	void createIgnoresClientSuppliedUserId() throws Exception {
		when(useCase.create(any())).thenReturn(order());

		mockMvc.perform(post(BASE).with(user(OWNER))
				.contentType(MediaType.APPLICATION_JSON)
				.content(VALID_BODY.replaceFirst("\\{", "{\"userId\":\"" + UUID.randomUUID() + "\",")))
				.andExpect(status().isCreated());

		ArgumentCaptor<CreateOrderCommand> captor = ArgumentCaptor.forClass(CreateOrderCommand.class);
		verify(useCase).create(captor.capture());
		assertThat(captor.getValue().userId()).isEqualTo(OWNER);
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor user(UUID subject) {
		return jwt()
				.jwt(token -> token.subject(subject.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_USER"));
	}

	private static Order order() {
		return new Order(ORDER_ID, OWNER,
				List.of(new OrderLine(UUID.randomUUID(), "SKU-1", "Widget", new BigDecimal("9.99"), "USD", 2)),
				OrderStatus.CREATED, new BigDecimal("19.98"), "USD", null, null, NOW, NOW);
	}
}
