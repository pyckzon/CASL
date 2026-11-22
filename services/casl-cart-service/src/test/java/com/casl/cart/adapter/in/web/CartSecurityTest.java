package com.casl.cart.adapter.in.web;

import com.casl.cart.application.port.in.CartUseCase;
import com.casl.cart.config.SecurityConfig;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Zero-trust: JWT validado en el servicio y propiedad del carrito. */
@WebMvcTest(value = CartController.class, excludeAutoConfiguration = {
		org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class,
		org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration.class,
		org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class })
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class CartSecurityTest {

	private static final String BASE = "/api/cart/v1/carts";
	private static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID CART_ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000003");
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CartUseCase useCase;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@Test
	void requestWithoutTokenReturns401ProblemJson() throws Exception {
		mockMvc.perform(get(BASE + "/{id}", CART_ID))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		verifyNoInteractions(useCase);
	}

	@Test
	void tokenWithoutUserRoleReturns403() throws Exception {
		mockMvc.perform(get(BASE + "/{id}", CART_ID).with(jwt()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		verifyNoInteractions(useCase);
	}

	@Test
	void userCannotReadAnotherUsersCart() throws Exception {
		when(useCase.get(CART_ID)).thenReturn(cart());

		mockMvc.perform(get(BASE + "/{id}", CART_ID).with(user(UUID.randomUUID())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
	}

	@Test
	void ownerCanReadHisCart() throws Exception {
		when(useCase.get(CART_ID)).thenReturn(cart());

		mockMvc.perform(get(BASE + "/{id}", CART_ID).with(user(OWNER)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(OWNER.toString()));
	}

	@Test
	void createIgnoresClientSuppliedUserId() throws Exception {
		when(useCase.create(OWNER)).thenReturn(cart());

		mockMvc.perform(post(BASE).param("userId", UUID.randomUUID().toString()).with(user(OWNER)))
				.andExpect(status().isCreated());
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor user(UUID subject) {
		return jwt()
				.jwt(token -> token.subject(subject.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_USER"));
	}

	private static Cart cart() {
		return new Cart(CART_ID, OWNER, List.of(new CartItem(UUID.randomUUID(), "SKU-1", "Widget",
				new BigDecimal("9.99"), "USD", 1)), NOW, NOW);
	}
}
