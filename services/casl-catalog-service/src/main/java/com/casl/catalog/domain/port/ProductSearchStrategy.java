package com.casl.catalog.domain.port;

import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductSearchCriteria;

/**
 * Estrategia de búsqueda (Strategy). Cada implementación resuelve los criterios
 * de una forma distinta; el caso de uso compone o selecciona la estrategia sin
 * conocer los detalles de persistencia.
 */
public interface ProductSearchStrategy {
	PageResult<Product> search(ProductSearchCriteria criteria);
}
