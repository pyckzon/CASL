package com.casl.catalog.application.service;

import com.casl.catalog.application.command.AdjustStockCommand;
import com.casl.catalog.application.command.CreateProductCommand;
import com.casl.catalog.application.command.ReleaseStockCommand;
import com.casl.catalog.application.command.ReserveStockCommand;
import com.casl.catalog.application.command.UpdateProductCommand;
import com.casl.catalog.application.factory.ProductFactory;
import com.casl.catalog.application.port.in.ProductUseCase;
import com.casl.catalog.domain.exception.ProductNotFoundException;
import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductSearchCriteria;
import com.casl.catalog.domain.port.ProductRepository;
import com.casl.catalog.domain.port.ProductSearchStrategy;
import com.casl.catalog.domain.port.StockPolicy;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Log4j2
@Service
@Transactional(readOnly = true)
public class ProductApplicationService implements ProductUseCase {

	private final ProductRepository repository;
	private final ProductSearchStrategy searchStrategy;
	private final StockPolicy stockPolicy;
	private final Clock clock;

	public ProductApplicationService(ProductRepository repository, ProductSearchStrategy searchStrategy,
			StockPolicy stockPolicy, Clock clock) {
		this.repository = repository;
		this.searchStrategy = searchStrategy;
		this.stockPolicy = stockPolicy;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Product create(CreateProductCommand command) {
		Product product = ProductFactory.create(command, repository, clock);
		Product saved = repository.save(product);
		log.info("Product created id={} sku={}", saved.id(), saved.sku().value());
		return saved;
	}

	@Override
	public Product getById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}

	@Override
	public Product getBySku(String sku) {
		return repository.findBySku(com.casl.catalog.domain.model.Sku.of(sku))
				.orElseThrow(() -> new ProductNotFoundException(sku));
	}

	@Override
	public PageResult<Product> search(ProductSearchCriteria criteria) {
		return searchStrategy.search(criteria);
	}

	@Override
	@Transactional
	public Product update(UUID id, UpdateProductCommand command) {
		Product current = getById(id);
		Product updated = ProductFactory.applyUpdate(current, command, clock);
		Product saved = repository.save(updated);
		log.info("Product updated id={} sku={}", saved.id(), saved.sku().value());
		return saved;
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		Product product = getById(id);
		repository.deleteById(product.id());
		log.info("Product deleted id={}", product.id());
	}

	@Override
	@Transactional
	public Product adjustStock(AdjustStockCommand command) {
		Product current = getById(command.productId());
		Product adjusted = current.adjustStock(command.newQuantity(), clock.instant());
		Product saved = repository.save(adjusted);
		log.info("Stock adjusted id={} quantity={}", saved.id(), saved.stockQuantity());
		return saved;
	}

	@Override
	@Transactional
	public Product reserve(ReserveStockCommand command) {
		Product current = getById(command.productId());
		Product reserved = current.reserve(command.quantity(), stockPolicy, clock.instant());
		Product saved = repository.save(reserved);
		log.info("Stock reserved id={} quantity={} reserved={}", saved.id(), command.quantity(), saved.reservedQuantity());
		return saved;
	}

	@Override
	@Transactional
	public Product release(ReleaseStockCommand command) {
		Product current = getById(command.productId());
		Product released = current.release(command.quantity(), clock.instant());
		Product saved = repository.save(released);
		log.info("Stock released id={} quantity={}", saved.id(), command.quantity());
		return saved;
	}
}
