package com.casl.catalog.application.service;

import com.casl.catalog.domain.model.PageResult;
import com.casl.catalog.domain.model.Product;
import com.casl.catalog.domain.model.ProductSearchCriteria;
import com.casl.catalog.domain.model.Sku;
import com.casl.catalog.domain.port.ProductRepository;
import com.casl.catalog.domain.port.ProductSearchStrategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryProductRepository implements ProductRepository {

	private final Map<UUID, Product> products = new LinkedHashMap<>();

	@Override
	public Product save(Product product) {
		products.put(product.id(), product);
		return product;
	}

	@Override
	public Optional<Product> findById(UUID id) {
		return Optional.ofNullable(products.get(id));
	}

	@Override
	public Optional<Product> findBySku(Sku sku) {
		return products.values().stream().filter(p -> p.sku().equals(sku)).findFirst();
	}

	@Override
	public boolean existsBySku(Sku sku) {
		return findBySku(sku).isPresent();
	}

	@Override
	public PageResult<Product> findAll(int page, int size) {
		List<Product> sorted = products.values().stream()
				.sorted(Comparator.comparing(Product::createdAt).reversed())
				.toList();
		return paginate(sorted, page, size);
	}

	@Override
	public PageResult<Product> search(ProductSearchCriteria criteria, int page, int size) {
		List<Product> filtered = products.values().stream()
				.filter(p -> criteria.name() == null || p.name().toLowerCase().contains(criteria.name().toLowerCase()))
				.filter(p -> criteria.categoryId() == null || criteria.categoryId().equals(p.categoryId()))
				.filter(p -> criteria.minPrice() == null || p.price().amount().compareTo(criteria.minPrice()) >= 0)
				.filter(p -> criteria.maxPrice() == null || p.price().amount().compareTo(criteria.maxPrice()) <= 0)
				.filter(p -> criteria.status() == null || criteria.status() == p.status())
				.sorted(Comparator.comparing(Product::createdAt).reversed())
				.toList();
		return paginate(filtered, page, size);
	}

	@Override
	public void deleteById(UUID id) {
		products.remove(id);
	}

	private static PageResult<Product> paginate(List<Product> all, int page, int size) {
		int from = Math.min(page * size, all.size());
		int to = Math.min(from + size, all.size());
		return new PageResult<>(all.subList(from, to), page, size, all.size());
	}
}

class InMemoryCategoryRepository implements com.casl.catalog.domain.port.CategoryRepository {

	private final Map<UUID, com.casl.catalog.domain.model.Category> categories = new LinkedHashMap<>();

	@Override
	public com.casl.catalog.domain.model.Category save(com.casl.catalog.domain.model.Category category) {
		categories.put(category.id(), category);
		return category;
	}

	@Override
	public Optional<com.casl.catalog.domain.model.Category> findById(UUID id) {
		return Optional.ofNullable(categories.get(id));
	}

	@Override
	public List<com.casl.catalog.domain.model.Category> findAll() {
		return new ArrayList<>(categories.values());
	}

	@Override
	public boolean existsByName(String name) {
		return categories.values().stream().anyMatch(c -> c.name().equalsIgnoreCase(name));
	}

	@Override
	public void deleteById(UUID id) {
		categories.remove(id);
	}
}

class InMemoryProductSearchStrategy implements ProductSearchStrategy {

	private final ProductRepository repository;

	InMemoryProductSearchStrategy(ProductRepository repository) {
		this.repository = repository;
	}

	@Override
	public PageResult<Product> search(ProductSearchCriteria criteria) {
		return repository.search(criteria, criteria.page(), criteria.size());
	}
}
