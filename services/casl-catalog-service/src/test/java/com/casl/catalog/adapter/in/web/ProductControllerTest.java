package com.casl.catalog.adapter.in.web;

import com.casl.catalog.application.port.in.ProductUseCase;
import com.casl.catalog.domain.exception.InsufficientStockException;
import com.casl.catalog.domain.exception.ProductNotFoundException;
import com.casl.catalog.domain.model.Money;
import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductStatus;
import com.casl.catalog.domain.model.Sku;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

	private static final String BASE = "/api/catalog/v1/products";
	private static final UUID ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000002");
	private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductUseCase useCase;

	private static Product product() {
		return new Product(ID, Sku.of("SKU-1"), "Widget", "desc", Money.of(new BigDecimal("9.99"), "USD"), null,
				ProductStatus.ACTIVE, 10, 2, NOW, NOW);
	}

	@Test
	void createReturns201WithLocation() throws Exception {
		when(useCase.create(any())).thenReturn(product());

		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
				{"sku":"sku-1","name":"Widget","price":9.99,"currency":"USD","initialStock":10}
				"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", BASE + "/" + ID))
				.andExpect(jsonPath("$.sku").value("SKU-1"))
				.andExpect(jsonPath("$.availableQuantity").value(8));
	}

	@Test
	void getUnknownReturns404ProblemJson() throws Exception {
		when(useCase.getById(ID)).thenThrow(new ProductNotFoundException(ID));

		mockMvc.perform(get(BASE + "/{id}", ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	void searchReturnsPage() throws Exception {
		when(useCase.search(any())).thenReturn(new PageResult<>(List.of(product()), 0, 20, 1));

		mockMvc.perform(get(BASE).param("name", "widget"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].sku").value("SKU-1"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void reserveInsufficientReturns409() throws Exception {
		when(useCase.reserve(any())).thenThrow(new InsufficientStockException("SKU-1", 100, 8));

		mockMvc.perform(post(BASE + "/{id}/stock/reserve", ID).contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":100}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
	}

	@Test
	void invalidBodyReturns400() throws Exception {
		mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"sku\":\"\",\"name\":\"\",\"price\":-1,\"currency\":\"US\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		verifyNoInteractions(useCase);
	}
}
