package com.casl.order;

import com.casl.order.domain.model.Order;
import com.casl.order.domain.model.OrderLine;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ResilienceIntegrationTest {

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

	@org.junit.jupiter.api.BeforeEach
	void clean() {
		jdbcTemplate.execute("DELETE FROM orders");
		// Reset circuit breaker state between tests
		CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("payment");
		cb.reset();
	}

	private String createOrder() throws Exception {
		String body = "{\"items\":[{\"productId\":\"11111111-0000-0000-0000-000000000001\",\"sku\":\"SKU-1\",\"name\":\"Widget\",\"unitPrice\":9.99,\"currency\":\"USD\",\"quantity\":2}]}";
		String response = mockMvc.perform(post("/api/orders/v1/orders").contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return new tools.jackson.databind.ObjectMapper().readTree(response).get("orderId").asText();
	}

	@Test
	void paymentFailureTriggersFallbackAndCancelsOrder() throws Exception {
		when(paymentGateway.charge(any())).thenThrow(new RuntimeException("connection refused"));

		String orderId = createOrder();

		mockMvc.perform(post("/api/orders/v1/orders/" + orderId + "/pay"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.failureReason").value(org.hamcrest.Matchers.containsString("payment service unavailable")));

		// Retry should have attempted 3 times (maxAttempts=3)
		verify(paymentGateway, times(3)).charge(any());
	}

	@Test
	void paymentTimeoutTriggersFallback() throws Exception {
		when(paymentGateway.charge(any())).thenAnswer(invocation -> {
			Thread.sleep(5000); // longer than timeout (3s)
			return com.casl.order.domain.model.PaymentResult.approved(java.util.UUID.randomUUID());
		});

		String orderId = createOrder();

		mockMvc.perform(post("/api/orders/v1/orders/" + orderId + "/pay"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void circuitBreakerOpensAfterRepeatedFailures() throws Exception {
		when(paymentGateway.charge(any())).thenThrow(new RuntimeException("service down"));

		// Trigger 5 failures to open the circuit breaker (minimumNumberOfCalls=5)
		for (int i = 0; i < 5; i++) {
			String orderId = createOrder();
			mockMvc.perform(post("/api/orders/v1/orders/" + orderId + "/pay"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("CANCELLED"));
		}

		CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("payment");
		assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

		// Next call should not invoke the gateway (circuit is open)
		org.mockito.Mockito.reset(paymentGateway);
		when(paymentGateway.charge(any())).thenThrow(new RuntimeException("should not be called"));
		String orderId = createOrder();
		mockMvc.perform(post("/api/orders/v1/orders/" + orderId + "/pay"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		// Gateway should NOT have been called (circuit open -> fallback directly)
		verify(paymentGateway, times(0)).charge(any());
	}

	@Test
	void metricsEndpointExposesResilience4j() throws Exception {
		mockMvc.perform(get("/actuator/metrics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.names").value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("resilience4j"))));
	}
}
