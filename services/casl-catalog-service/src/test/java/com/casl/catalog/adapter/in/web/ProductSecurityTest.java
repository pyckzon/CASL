package com.casl.catalog.adapter.in.web;

import com.casl.catalog.application.port.in.ProductUseCase;
import com.casl.catalog.config.SecurityConfig;
import com.casl.catalog.domain.model.Money;
import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductStatus;
import com.casl.catalog.domain.model.Sku;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica las reglas de autorización del catálogo: deny-by-default (401),
 * rol insuficiente (403) y acceso con el rol correcto.
 */
@WebMvcTest(value = ProductController.class, excludeAutoConfiguration = {
		org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
		org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class ProductSecurityTest {

	private static final String BASE = "/api/catalog/v1/products";
	private static final UUID ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000002");
	private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");
	private static final String VALID_BODY = """
			{"sku":"sku-1","name":"Widget","price":9.99,"currency":"USD","initialStock":10}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductUseCase useCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void requestWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get(BASE))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		verifyNoInteractions(useCase);
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCanReadProducts() throws Exception {
		when(useCase.search(any())).thenReturn(new PageResult<>(List.of(product()), 0, 20, 1));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "USER")
	void userCannotCreateProducts() throws Exception {
		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		verifyNoInteractions(useCase);
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminCanCreateProducts() throws Exception {
		when(useCase.create(any())).thenReturn(product());

		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isCreated());
	}

	private static Product product() {
		return new Product(ID, Sku.of("SKU-1"), "Widget", "desc", Money.of(new BigDecimal("9.99"), "USD"), null,
				ProductStatus.ACTIVE, 10, 0, CREATED, CREATED);
	}
}
