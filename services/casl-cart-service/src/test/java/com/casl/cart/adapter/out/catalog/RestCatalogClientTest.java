package com.casl.cart.adapter.out.catalog;

import com.casl.cart.domain.model.CatalogProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpStatus;

@RestClientTest(RestCatalogClient.class)
class RestCatalogClientTest {

	private static final UUID ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000004");

	@Autowired
	private RestCatalogClient client;

	@Autowired
	private MockRestServiceServer server;

	@Test
	void findByIdMapsCatalogResponse() {
		server.expect(requestTo("http://localhost:8670/api/catalog/v1/products/" + ID))
				.andRespond(withSuccess("""
						{"id":"%s","sku":"SKU-1","name":"Widget","price":9.99,"currency":"USD","status":"ACTIVE","stockQuantity":10,"reservedQuantity":2,"availableQuantity":8}
						""".formatted(ID), MediaType.APPLICATION_JSON));

		CatalogProduct product = client.findById(ID).orElseThrow();

		assertThat(product.sku()).isEqualTo("SKU-1");
		assertThat(product.unitPrice()).isEqualByComparingTo("9.99");
		assertThat(product.availableQuantity()).isEqualTo(8);
	}

	@Test
	void findByIdReturnsEmptyOn404() {
		server.expect(requestTo("http://localhost:8670/api/catalog/v1/products/" + ID))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThat(client.findById(ID)).isEmpty();
	}
}
