package com.casl.cart.domain.port;

import com.casl.cart.domain.model.CatalogProduct;

import java.util.Optional;
import java.util.UUID;

/**
 * Anti-corruption layer hacia el catálogo. El carrito no conoce el modelo del
 * catálogo; solo esta vista mínima.
 */
public interface CatalogClient {
	Optional<CatalogProduct> findById(UUID productId);
	Optional<CatalogProduct> findBySku(String sku);
}
