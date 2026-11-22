package com.casl.cart;

import com.casl.cart.domain.model.CatalogProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CartApiIntegrationTest {

	private static final String BASE = "/api/cart/v1/carts";
	private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID P1 = UUID.fromString("11111111-0000-0000-0000-000000000001");

	@Container
	static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
			.withExposedPorts(6379);

	@DynamicPropertySource
	static void props(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
		registry.add("casl.catalog.base-url", () -> "http://localhost:9999");
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private com.casl.cart.domain.port.CatalogClient catalogClient;

	@BeforeEach
	void stubCatalog() {
		when(catalogClient.findById(P1)).thenReturn(java.util.Optional.of(
				new CatalogProduct(P1, "SKU-1", "Widget", new BigDecimal("9.99"), "USD", 10, "ACTIVE")));
	}

	@Test
	void createAddAndRetrieveCart() throws Exception {
		String cartJson = mockMvc.perform(post(BASE).with(user()))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String cartId = new tools.jackson.databind.ObjectMapper().readTree(cartJson).get("cartId").asText();

		mockMvc.perform(post(BASE + "/" + cartId + "/items").with(user())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productId\":\"" + P1 + "\",\"quantity\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(USER_ID.toString()))
				.andExpect(jsonPath("$.totalItems").value(2))
				.andExpect(jsonPath("$.totalAmount").value(19.98));

		mockMvc.perform(get(BASE + "/" + cartId).with(user()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].sku").value("SKU-1"));
	}

	@Test
	void addItemUnavailableReturns409() throws Exception {
		when(catalogClient.findById(P1)).thenReturn(java.util.Optional.of(
				new CatalogProduct(P1, "SKU-1", "Widget", new BigDecimal("9.99"), "USD", 0, "ACTIVE")));

		String cartJson = mockMvc.perform(post(BASE).with(user()))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String cartId = new tools.jackson.databind.ObjectMapper().readTree(cartJson).get("cartId").asText();

		mockMvc.perform(post(BASE + "/" + cartId + "/items").with(user())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productId\":\"" + P1 + "\",\"quantity\":1}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_AVAILABLE"));
	}

	private static RequestPostProcessor user() {
		return jwt()
				.jwt(token -> token.subject(USER_ID.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_USER"));
	}
}
