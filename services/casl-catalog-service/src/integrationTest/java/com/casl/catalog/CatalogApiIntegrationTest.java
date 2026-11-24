package com.casl.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "casl.security.user-status.enabled=false")
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
@Testcontainers
class CatalogApiIntegrationTest {

	private static final String BASE = "/api/catalog/v1/products";

	@Container
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
			DockerImageName.parse("postgres:17-alpine"));

	@Container
	static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void clean() {
		jdbcTemplate.execute("DELETE FROM products");
		jdbcTemplate.execute("DELETE FROM categories");
	}

	@Test
	void createAndRetrieveProduct() throws Exception {
		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
				{"sku":"SKU-1","name":"Widget","price":9.99,"currency":"USD","initialStock":10}
				"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sku").value("SKU-1"))
				.andExpect(jsonPath("$.availableQuantity").value(10));

		mockMvc.perform(get(BASE + "/sku/SKU-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Widget"));
	}

	@Test
	void searchAndStockFlow() throws Exception {
		String createResponse = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
				{"sku":"SKU-1","name":"Widget","price":9.99,"currency":"USD","initialStock":10}
				"""))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		tools.jackson.databind.JsonNode node = new tools.jackson.databind.ObjectMapper().readTree(createResponse);
		String id = node.get("id").asText();

		mockMvc.perform(get(BASE).param("name", "widget"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(post(BASE + "/" + id + "/stock/reserve").contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":3}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reservedQuantity").value(3))
				.andExpect(jsonPath("$.availableQuantity").value(7));

		mockMvc.perform(post(BASE + "/" + id + "/stock/release").contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":1}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reservedQuantity").value(2));

		mockMvc.perform(post(BASE + "/" + id + "/stock/adjust").contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":20}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stockQuantity").value(20));
	}

	@Test
	void categoryCrud() throws Exception {
		mockMvc.perform(post("/api/catalog/v1/categories").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Electronics\",\"description\":\"desc\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Electronics"));

		mockMvc.perform(get("/api/catalog/v1/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}
}
