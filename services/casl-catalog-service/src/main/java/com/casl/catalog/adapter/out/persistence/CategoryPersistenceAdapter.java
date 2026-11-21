package com.casl.catalog.adapter.out.persistence;

import com.casl.catalog.domain.model.Category;
import com.casl.catalog.domain.port.CategoryRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository
public class CategoryPersistenceAdapter implements CategoryRepository {

	private final CategoryJpaRepository repository;

	public CategoryPersistenceAdapter(CategoryJpaRepository repository) {
		this.repository = repository;
	}

	@Override
	public Category save(Category category) {
		return CategoryPersistenceMapper.toDomain(repository.saveAndFlush(CategoryPersistenceMapper.toEntity(category)));
	}

	@Override
	public Optional<Category> findById(UUID id) {
		return repository.findById(id).map(CategoryPersistenceMapper::toDomain);
	}

	@Override
	public List<Category> findAll() {
		return repository.findAll().stream().map(CategoryPersistenceMapper::toDomain).toList();
	}

	@Override
	public boolean existsByName(String name) {
		return repository.existsByName(name);
	}

	@Override
	public void deleteById(UUID id) {
		repository.deleteById(id);
	}
}
