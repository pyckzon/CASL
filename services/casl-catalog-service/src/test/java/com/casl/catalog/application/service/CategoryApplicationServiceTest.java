package com.casl.catalog.application.service;

import com.casl.catalog.application.command.CreateCategoryCommand;
import com.casl.catalog.domain.exception.CategoryNotFoundException;
import com.casl.catalog.domain.exception.InvalidCategoryException;
import com.casl.catalog.domain.model.Category;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryApplicationServiceTest {

	private final InMemoryCategoryRepository repository = new InMemoryCategoryRepository();
	private final CategoryApplicationService service = new CategoryApplicationService(repository,
			Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC));

	@Test
	void createAndList() {
		Category created = service.create(new CreateCategoryCommand("Electronics", "desc"));

		assertThat(created.id()).isNotNull();
		assertThat(service.list()).hasSize(1);
		assertThat(service.getById(created.id())).isEqualTo(created);
	}

	@Test
	void createRejectsDuplicateName() {
		service.create(new CreateCategoryCommand("Electronics", null));

		assertThatThrownBy(() -> service.create(new CreateCategoryCommand("electronics", null)))
				.isInstanceOf(InvalidCategoryException.class);
	}

	@Test
	void deleteRemovesCategory() {
		Category created = service.create(new CreateCategoryCommand("Electronics", null));

		service.delete(created.id());

		assertThatThrownBy(() -> service.getById(created.id())).isInstanceOf(CategoryNotFoundException.class);
	}
}
