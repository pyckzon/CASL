package com.casl.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "casl.security.user-status.enabled=false")
@AutoConfigureMockMvc
@Testcontainers
class PaymentApiIntegrationTest {

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
			DockerImageName.parse("postgres:17-alpine"));

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@org.junit.jupiter.api.BeforeEach
	void clean() {
		jdbcTemplate.execute("DELETE FROM payments");
	}

	@Test
	void chargeAndIdempotency() throws Exception {
		String body = "{\"amount\":9.99,\"currency\":\"USD\",\"idempotencyKey\":\"key-1\"}";

		String first = mockMvc.perform(post("/api/payments/v1/payments").with(serviceToken())
						.header("Idempotency-Key", "key-1")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("APPROVED"))
				.andReturn().getResponse().getContentAsString();

		String paymentId = new tools.jackson.databind.ObjectMapper().readTree(first).get("paymentId").asText();

		// Repeat with same key -> same payment, no duplicate charge
		mockMvc.perform(post("/api/payments/v1/payments").with(serviceToken())
						.header("Idempotency-Key", "key-1")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.paymentId").value(paymentId));

		mockMvc.perform(get("/api/payments/v1/payments/" + paymentId).with(serviceToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("APPROVED"));
	}

	@Test
	void chargeRejectsHighAmount() throws Exception {
		mockMvc.perform(post("/api/payments/v1/payments").with(serviceToken())
						.header("Idempotency-Key", "key-2")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":2000.00,\"currency\":\"USD\",\"idempotencyKey\":\"key-2\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("REJECTED"));
	}

	@Test
	void refundApprovedPayment() throws Exception {
		String body = "{\"amount\":9.99,\"currency\":\"USD\",\"idempotencyKey\":\"key-3\"}";
		String response = mockMvc.perform(post("/api/payments/v1/payments").with(serviceToken())
						.header("Idempotency-Key", "key-3")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String paymentId = new tools.jackson.databind.ObjectMapper().readTree(response).get("paymentId").asText();

		mockMvc.perform(post("/api/payments/v1/payments/" + paymentId + "/refund").with(serviceToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("REFUNDED"));
	}

	private static RequestPostProcessor serviceToken() {
		return jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
				.claim("preferred_username", "service-account-casl-order-service")
				.claim("client_id", "casl-order-service"));
	}
}
