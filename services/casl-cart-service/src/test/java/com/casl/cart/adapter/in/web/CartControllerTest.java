package com.casl.cart.adapter.in.web;

import com.casl.cart.application.port.in.CartUseCase;
import com.casl.cart.config.SecurityConfig;
import com.casl.cart.domain.exception.CartNotFoundException;
import com.casl.cart.domain.exception.ProductNotAvailableException;
import com.casl.cart.domain.model.Cart;
import com.casl.cart.domain.model.CartItem;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = CartController.class, excludeAutoConfiguration = {
		org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
		org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class CartControllerTest {

	private static final String BASE = "/api/cart/v1/carts";
	private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID CART_ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000003");
	private static final UUID P1 = UUID.fromString("11111111-0000-0000-0000-000000000001");
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CartUseCase useCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void createUsesAuthenticatedSubjectAsOwner() throws Exception {
		when(useCase.create(USER_ID)).thenReturn(cart());

		mockMvc.perform(post(BASE).with(user(USER_ID)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cartId").value(CART_ID.toString()))
				.andExpect(jsonPath("$.userId").value(USER_ID.toString()))
				.andExpect(jsonPath("$.totalItems").value(2));
	}

	@Test
	void getReturnsOwnCart() throws Exception {
		when(useCase.get(CART_ID)).thenReturn(cart());

		mockMvc.perform(get(BASE + "/{id}", CART_ID).with(user(USER_ID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].sku").value("SKU-1"));
	}

	@Test
	void getUnknownReturns404() throws Exception {
		when(useCase.get(CART_ID)).thenThrow(new CartNotFoundException(CART_ID));

		mockMvc.perform(get(BASE + "/{id}", CART_ID).with(user(USER_ID)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CART_NOT_FOUND"));
	}

	@Test
	void addItemReturnsUpdatedCart() throws Exception {
		when(useCase.get(CART_ID)).thenReturn(cart());
		when(useCase.addItem(any())).thenReturn(cart());

		mockMvc.perform(post(BASE + "/{id}/items", CART_ID).with(user(USER_ID))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":\"11111111-0000-0000-0000-000000000001\",\"quantity\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(2));
	}

	@Test
	void addItemUnavailableReturns409() throws Exception {
		when(useCase.get(CART_ID)).thenReturn(cart());
		when(useCase.addItem(any())).thenThrow(new ProductNotAvailableException("SKU-1"));

		mockMvc.perform(post(BASE + "/{id}/items", CART_ID).with(user(USER_ID))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":\"11111111-0000-0000-0000-000000000001\",\"quantity\":2}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_AVAILABLE"));
	}

	@Test
	void invalidBodyReturns400() throws Exception {
		mockMvc.perform(post(BASE + "/{id}/items", CART_ID).with(user(USER_ID))
				.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		verifyNoInteractions(useCase);
	}

	@Test
	void removeItemChecksOwnership() throws Exception {
		when(useCase.get(CART_ID)).thenReturn(cart());
		when(useCase.removeItem(any())).thenReturn(cart());

		mockMvc.perform(delete(BASE + "/{id}/items/{productId}", CART_ID, P1).with(user(USER_ID)))
				.andExpect(status().isOk());
	}

	@Test
	void updateQuantityChecksOwnership() throws Exception {
		when(useCase.get(CART_ID)).thenReturn(cart());
		when(useCase.updateQuantity(any())).thenReturn(cart());

		mockMvc.perform(put(BASE + "/{id}/items/{productId}", CART_ID, P1).with(user(USER_ID))
				.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":3}"))
				.andExpect(status().isOk());
	}

	private static RequestPostProcessor user(UUID subject) {
		return jwt()
				.jwt(token -> token.subject(subject.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_USER"));
	}

	private static Cart cart() {
		return new Cart(CART_ID, USER_ID,
				List.of(new CartItem(P1, "SKU-1", "Widget", new BigDecimal("9.99"), "USD", 2)), NOW, NOW);
	}
}
