package com.casl.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestPropertySource(properties = "casl.payment.simulated=true")
class OrderApiIntegrationTest {

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
		jdbcTemplate.execute("DELETE FROM orders");
	}

	@Test
	void createAndGetOrder() throws Exception {
		mockMvc.perform(post("/api/orders/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"items":[{"productId":"11111111-0000-0000-0000-000000000001","sku":"SKU-1","name":"Widget","unitPrice":9.99,"currency":"USD","quantity":2}]}
				"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CREATED"))
				.andExpect(jsonPath("$.totalAmount").value(19.98));
	}

	@Test
	void payApprovesAndMarksPaid() throws Exception {
		String response = mockMvc.perform(post("/api/orders/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"items":[{"productId":"11111111-0000-0000-0000-000000000001","sku":"SKU-1","name":"Widget","unitPrice":9.99,"currency":"USD","quantity":2}]}
				"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String orderId = new tools.jackson.databind.ObjectMapper().readTree(response).get("orderId").asText();

		mockMvc.perform(post("/api/orders/v1/orders/" + orderId + "/pay"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAID"))
				.andExpect(jsonPath("$.paymentId").exists());
	}

	@Test
	void payRejectsAndCancelsWhenAmountTooHigh() throws Exception {
		String response = mockMvc.perform(post("/api/orders/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
				{"items":[{"productId":"11111111-0000-0000-0000-000000000001","sku":"SKU-1","name":"Widget","unitPrice":9.99,"currency":"USD","quantity":200}]}
				"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String orderId = new tools.jackson.databind.ObjectMapper().readTree(response).get("orderId").asText();

		mockMvc.perform(post("/api/orders/v1/orders/" + orderId + "/pay"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}
}
