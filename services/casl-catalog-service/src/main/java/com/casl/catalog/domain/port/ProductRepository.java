package com.casl.catalog.domain.port;

import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.Sku;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
	Product save(Product product);
	Optional<Product> findById(UUID id);
	Optional<Product> findBySku(Sku sku);
	boolean existsBySku(Sku sku);
	com.casl.catalog.domain.model.PageResult<Product> findAll(int page, int size);
	com.casl.catalog.domain.model.PageResult<Product> search(com.casl.catalog.domain.model.ProductSearchCriteria criteria, int page, int size);
	void deleteById(UUID id);
}
