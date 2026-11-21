package com.casl.catalog.adapter.out.persistence;

import com.casl.catalog.domain.model.Category;

final class CategoryPersistenceMapper {

	private CategoryPersistenceMapper() {
	}

	static CategoryJpaEntity toEntity(Category category) {
		return new CategoryJpaEntity(category.id(), category.name(), category.description(), category.createdAt(),
				category.updatedAt());
	}

	static Category toDomain(CategoryJpaEntity entity) {
		return new Category(entity.getId(), entity.getName(), entity.getDescription(), entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
