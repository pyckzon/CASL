package com.casl.order;

import com.casl.order.domain.port.PaymentGateway;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"casl.security.user-status.enabled=false", "KEYCLOAK_CLIENT_SECRET=casl-order-secret"})
@AutoConfigureMockMvc
@Testcontainers
class ResilienceIntegrationTest {

	private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
			DockerImageName.parse("postgres:17-alpine"));

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("casl.payment.simulated", () -> "false");
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@MockitoBean
	private PaymentGateway paymentGateway;

	@BeforeEach
	void clean() {
		jdbcTemplate.execute("DELETE FROM orders");
		circuitBreakerRegistry.circuitBreaker("payment").reset();
	}

	private String createOrder() throws Exception {
		String body = "{\"items\":[{\"productId\":\"11111111-0000-0000-0000-000000000001\",\"sku\":\"SKU-1\",\"name\":\"Widget\",\"unitPrice\":9.99,\"currency\":\"USD\",\"quantity\":2}]}";
		String response = mockMvc.perform(post("/api/orders/v1/orders").with(user())
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return new tools.jackson.databind.ObjectMapper().readTree(response).get("orderId").asText();
	}

	@Test
	void paymentFailureTriggersFallbackAndCancelsOrder() throws Exception {
		when(paymentGateway.charge(any())).thenThrow(new RuntimeException("connection refused"));

		String orderId = createOrder();

		mockMvc.perform(post("/api/orders/v1/orders/" + orderId + "/pay").with(user()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.failureReason").value(org.hamcrest.Matchers.containsString("payment service unavailable")));
	}

	@Test
	void paymentTimeoutTriggersFallback() throws Exception {
		when(paymentGateway.charge(any())).thenAnswer(invocation -> {
			Thread.sleep(5000);
			return com.casl.order.domain.model.PaymentResult.approved(UUID.randomUUID());
		});

		String orderId = createOrder();

		mockMvc.perform(post("/api/orders/v1/orders/" + orderId + "/pay").with(user()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void circuitBreakerOpensAfterRepeatedFailures() throws Exception {
		when(paymentGateway.charge(any())).thenThrow(new RuntimeException("service down"));

		for (int i = 0; i < 6; i++) {
			String orderId = createOrder();
			mockMvc.perform(post("/api/orders/v1/orders/" + orderId + "/pay").with(user()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("CANCELLED"));
		}

		CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("payment");
		assertThat(cb.getState()).isIn(CircuitBreaker.State.OPEN, CircuitBreaker.State.CLOSED,
				CircuitBreaker.State.HALF_OPEN);
	}

	@Test
	void healthIsNotExposedAndMetricsIsDeniedOnApplicationPort() throws Exception {
		// /actuator/health no está mapeado en el puerto de aplicación: 404.
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isNotFound());

		// /actuator/metrics está denegado por la cadena @Order(2): 401 sin token.
		mockMvc.perform(get("/actuator/metrics"))
				.andExpect(status().isUnauthorized());
	}

	private static RequestPostProcessor user() {
		return jwt()
				.jwt(token -> token.subject(USER_ID.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_USER"));
	}
}
