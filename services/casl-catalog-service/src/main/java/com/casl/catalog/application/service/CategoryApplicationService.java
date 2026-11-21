package com.casl.catalog.application.service;

import com.casl.catalog.application.command.CreateCategoryCommand;
import com.casl.catalog.application.port.in.CategoryUseCase;
import com.casl.catalog.domain.exception.CategoryNotFoundException;
import com.casl.catalog.domain.exception.InvalidCategoryException;
import com.casl.catalog.domain.model.Category;
import com.casl.catalog.domain.port.CategoryRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Log4j2
@Service
@Transactional(readOnly = true)
public class CategoryApplicationService implements CategoryUseCase {

	private final CategoryRepository repository;
	private final Clock clock;

	public CategoryApplicationService(CategoryRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Category create(CreateCategoryCommand command) {
		if (repository.existsByName(command.name())) {
			throw new InvalidCategoryException("category name already exists: " + command.name());
		}
		Instant now = clock.instant();
		Category category = new Category(UUID.randomUUID(), command.name(), command.description(), now, now);
		Category saved = repository.save(category);
		log.info("Category created id={} name={}", saved.id(), saved.name());
		return saved;
	}

	@Override
	public Category getById(UUID id) {
		return repository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
	}

	@Override
	public List<Category> list() {
		return repository.findAll();
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		Category category = getById(id);
		repository.deleteById(category.id());
		log.info("Category deleted id={}", category.id());
	}
}
