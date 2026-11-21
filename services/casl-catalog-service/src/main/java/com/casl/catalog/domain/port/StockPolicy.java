package com.casl.catalog.domain.port;

import com.casl.catalog.domain.model.Product;

/**
 * Política de reserva de stock. El agregado consulta a la política antes de
 * reservar; añadir una política nueva (pesimista, backorder) no modifica el
 * dominio ni el caso de uso.
 */
public interface StockPolicy {
	boolean canReserve(Product product, int quantity);
}
