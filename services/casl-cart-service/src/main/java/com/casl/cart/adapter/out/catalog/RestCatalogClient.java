package com.casl.cart.adapter.out.catalog;

import com.casl.cart.domain.model.CatalogProduct;
import com.casl.cart.domain.port.CatalogClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class RestCatalogClient implements CatalogClient {

	private final RestClient restClient;

	public RestCatalogClient(RestClient.Builder builder, @Value("${casl.catalog.base-url:http://localhost:8670}") String baseUrl) {
		this.restClient = builder.baseUrl(baseUrl).build();
	}

	@Override
	public Optional<CatalogProduct> findById(UUID productId) {
		return fetch("/api/catalog/v1/products/" + productId);
	}

	@Override
	public Optional<CatalogProduct> findBySku(String sku) {
		return fetch("/api/catalog/v1/products/sku/" + sku);
	}

	private Optional<CatalogProduct> fetch(String path) {
		try {
			CatalogProductResponse response = restClient.get().uri(path).retrieve().body(CatalogProductResponse.class);
			if (response == null) {
				return Optional.empty();
			}
			return Optional.of(new CatalogProduct(response.id(), response.sku(), response.name(), response.price(),
					response.currency(), response.availableQuantity(), response.status()));
		} catch (HttpClientErrorException exception) {
			if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
				return Optional.empty();
			}
			throw exception;
		}
	}

	public record CatalogProductResponse(
			UUID id,
			String sku,
			String name,
			BigDecimal price,
			String currency,
			String status,
			int stockQuantity,
			int reservedQuantity,
			int availableQuantity) {
	}
}
