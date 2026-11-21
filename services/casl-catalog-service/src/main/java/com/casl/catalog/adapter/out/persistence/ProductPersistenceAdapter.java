package com.casl.catalog.adapter.out.persistence;

import com.casl.catalog.domain.exception.DuplicateSkuException;
import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductSearchCriteria;
import com.casl.catalog.domain.model.Sku;
import com.casl.catalog.domain.port.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProductPersistenceAdapter implements ProductRepository {

	private final ProductJpaRepository repository;

	public ProductPersistenceAdapter(ProductJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Product save(Product product) {
		try {
			ProductJpaEntity saved = repository.saveAndFlush(ProductPersistenceMapper.toEntity(product));
			return ProductPersistenceMapper.toDomain(saved);
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateSkuException(product.sku().value());
		}
	}

	@Override
	public Optional<Product> findById(UUID id) {
		return repository.findById(id).map(ProductPersistenceMapper::toDomain);
	}

	@Override
	public Optional<Product> findBySku(Sku sku) {
		return repository.findBySku(sku.value()).map(ProductPersistenceMapper::toDomain);
	}

	@Override
	public boolean existsBySku(Sku sku) {
		return repository.existsBySku(sku.value());
	}

	@Override
	public PageResult<Product> findAll(int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<ProductJpaEntity> result = repository.findAll(pageable);
		return toPageResult(result, page, size);
	}

	@Override
	public PageResult<Product> search(ProductSearchCriteria criteria, int page, int size) {
		Specification<ProductJpaEntity> spec = buildSpecification(criteria);
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<ProductJpaEntity> result = (spec == null)
				? repository.findAll(pageable)
				: repository.findAll(spec, pageable);
		return toPageResult(result, page, size);
	}

	@Override
	public void deleteById(UUID id) {
		repository.deleteById(id);
	}

	private static Specification<ProductJpaEntity> buildSpecification(ProductSearchCriteria criteria) {
		List<Specification<ProductJpaEntity>> parts = new ArrayList<>();
		if (criteria.name() != null && !criteria.name().isBlank()) {
			String pattern = "%" + criteria.name().trim().toLowerCase() + "%";
			parts.add((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern));
		}
		if (criteria.categoryId() != null) {
			parts.add((root, q, cb) -> cb.equal(root.get("categoryId"), criteria.categoryId()));
		}
		if (criteria.minPrice() != null) {
			parts.add((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("price"), criteria.minPrice()));
		}
		if (criteria.maxPrice() != null) {
			parts.add((root, q, cb) -> cb.lessThanOrEqualTo(root.get("price"), criteria.maxPrice()));
		}
		if (criteria.status() != null) {
			parts.add((root, q, cb) -> cb.equal(root.get("status"), criteria.status().name()));
		}
		if (parts.isEmpty()) {
			return null;
		}
		Specification<ProductJpaEntity> result = parts.get(0);
		for (int i = 1; i < parts.size(); i++) {
			result = result.and(parts.get(i));
		}
		return result;
	}

	private static PageResult<Product> toPageResult(Page<ProductJpaEntity> page, int pageRequested, int sizeRequested) {
		List<Product> content = page.getContent().stream()
				.map(ProductPersistenceMapper::toDomain)
				.toList();
		return new PageResult<>(content, pageRequested, sizeRequested, page.getTotalElements());
	}
}
