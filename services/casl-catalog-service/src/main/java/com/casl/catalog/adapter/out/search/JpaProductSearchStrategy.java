package com.casl.catalog.adapter.out.search;

import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductSearchCriteria;
import com.casl.catalog.domain.port.ProductRepository;
import com.casl.catalog.domain.port.ProductSearchStrategy;

/**
 * Implementación JPA del puerto de búsqueda (Strategy). Traduce los criterios a
 * una Specification y delega la paginación al repositorio.
 */
public class JpaProductSearchStrategy implements ProductSearchStrategy {

	private final ProductRepository repository;

	public JpaProductSearchStrategy(ProductRepository repository) {
		this.repository = repository;
	}

	@Override
	public PageResult<Product> search(ProductSearchCriteria criteria) {
		return repository.search(criteria, criteria.page(), criteria.size());
	}
}
