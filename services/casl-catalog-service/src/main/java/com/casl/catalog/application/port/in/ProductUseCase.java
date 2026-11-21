package com.casl.catalog.application.port.in;

import com.casl.catalog.application.command.AdjustStockCommand;
import com.casl.catalog.application.command.CreateProductCommand;
import com.casl.catalog.application.command.ReleaseStockCommand;
import com.casl.catalog.application.command.ReserveStockCommand;
import com.casl.catalog.application.command.UpdateProductCommand;
import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductSearchCriteria;

import java.util.UUID;

public interface ProductUseCase {
	Product create(CreateProductCommand command);
	Product getById(UUID id);
	Product getBySku(String sku);
	PageResult<Product> search(ProductSearchCriteria criteria);
	Product update(UUID id, UpdateProductCommand command);
	void delete(UUID id);
	Product adjustStock(AdjustStockCommand command);
	Product reserve(ReserveStockCommand command);
	Product release(ReleaseStockCommand command);
}
