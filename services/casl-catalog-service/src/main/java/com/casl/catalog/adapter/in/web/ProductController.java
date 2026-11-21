package com.casl.catalog.adapter.in.web;

import com.casl.catalog.application.command.AdjustStockCommand;
import com.casl.catalog.application.command.CreateProductCommand;
import com.casl.catalog.application.command.ReleaseStockCommand;
import com.casl.catalog.application.command.ReserveStockCommand;
import com.casl.catalog.application.command.UpdateProductCommand;
import com.casl.catalog.application.port.in.ProductUseCase;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductSearchCriteria;
import com.casl.catalog.domain.model.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/v1/products")
public class ProductController {

	private static final String BASE = "/api/catalog/v1/products/";

	private final ProductUseCase useCase;

	public ProductController(ProductUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping
	public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
		Product created = useCase.create(ProductWebMapper.toCreateCommand(request));
		return ResponseEntity.created(URI.create(BASE + created.id()))
				.body(ProductWebMapper.toResponse(created));
	}

	@GetMapping("/{id}")
	public ProductResponse get(@PathVariable UUID id) {
		return ProductWebMapper.toResponse(useCase.getById(id));
	}

	@GetMapping("/sku/{sku}")
	public ProductResponse getBySku(@PathVariable String sku) {
		return ProductWebMapper.toResponse(useCase.getBySku(sku));
	}

	@GetMapping
	public ProductPageResponse search(
			@RequestParam(required = false) @Size(max = 200) String name,
			@RequestParam(required = false) UUID categoryId,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		ProductSearchCriteria criteria = ProductWebMapper.toCriteria(name, categoryId, minPrice, maxPrice, status, page, size);
		return ProductWebMapper.toPageResponse(useCase.search(criteria));
	}

	@PutMapping("/{id}")
	public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
		return ProductWebMapper.toResponse(useCase.update(id, ProductWebMapper.toUpdateCommand(request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		useCase.delete(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/stock/adjust")
	public ProductResponse adjustStock(@PathVariable UUID id, @Valid @RequestBody StockQuantityRequest request) {
		return ProductWebMapper.toResponse(useCase.adjustStock(new AdjustStockCommand(id, request.quantity())));
	}

	@PostMapping("/{id}/stock/reserve")
	public ProductResponse reserve(@PathVariable UUID id, @Valid @RequestBody StockQuantityRequest request) {
		return ProductWebMapper.toResponse(useCase.reserve(new ReserveStockCommand(id, request.quantity())));
	}

	@PostMapping("/{id}/stock/release")
	public ProductResponse release(@PathVariable UUID id, @Valid @RequestBody StockQuantityRequest request) {
		return ProductWebMapper.toResponse(useCase.release(new ReleaseStockCommand(id, request.quantity())));
	}
}
