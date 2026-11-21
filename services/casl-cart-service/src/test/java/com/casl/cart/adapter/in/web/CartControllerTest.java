package com.casl.cart.adapter.in.web;

import com.casl.cart.application.port.in.CartUseCase;
import com.casl.cart.domain.exception.CartNotFoundException;
import com.casl.cart.domain.exception.ProductNotAvailableException;
import com.casl.cart.domain.model.Cart;
import com.casl.cart.domain.model.CartItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

	private static final String BASE = "/api/cart/v1/carts";
	private static final UUID CART_ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000003");
	private static final UUID P1 = UUID.fromString("11111111-0000-0000-0000-000000000001");
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CartUseCase useCase;

	private static Cart cart() {
		return new Cart(CART_ID, null, List.of(new CartItem(P1, "SKU-1", "Widget", new BigDecimal("9.99"), "USD", 2)),
				NOW, NOW);
	}

	@Test
	void createReturns201() throws Exception {
		when(useCase.create(null)).thenReturn(cart());

		mockMvc.perform(post(BASE))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.cartId").value(CART_ID.toString()))
				.andExpect(jsonPath("$.totalItems").value(2))
				.andExpect(jsonPath("$.totalAmount").value(19.98));
	}

	@Test
	void getReturnsCart() throws Exception {
		when(useCase.get(CART_ID)).thenReturn(cart());

		mockMvc.perform(get(BASE + "/{id}", CART_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].sku").value("SKU-1"));
	}

	@Test
	void getUnknownReturns404() throws Exception {
		when(useCase.get(CART_ID)).thenThrow(new CartNotFoundException(CART_ID));

		mockMvc.perform(get(BASE + "/{id}", CART_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CART_NOT_FOUND"));
	}

	@Test
	void addItemReturnsUpdatedCart() throws Exception {
		when(useCase.addItem(any())).thenReturn(cart());

		mockMvc.perform(post(BASE + "/{id}/items", CART_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":\"11111111-0000-0000-0000-000000000001\",\"quantity\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(2));
	}

	@Test
	void addItemUnavailableReturns409() throws Exception {
		when(useCase.addItem(any())).thenThrow(new ProductNotAvailableException("SKU-1"));

		mockMvc.perform(post(BASE + "/{id}/items", CART_ID).contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":\"11111111-0000-0000-0000-000000000001\",\"quantity\":2}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_AVAILABLE"));
	}

	@Test
	void invalidBodyReturns400() throws Exception {
		mockMvc.perform(post(BASE + "/{id}/items", CART_ID).contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		verifyNoInteractions(useCase);
	}
}
