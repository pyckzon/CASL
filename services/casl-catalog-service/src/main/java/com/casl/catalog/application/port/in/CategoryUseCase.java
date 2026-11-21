package com.casl.catalog.application.port.in;

import com.casl.catalog.application.command.CreateCategoryCommand;
import com.casl.catalog.domain.model.Category;

import java.util.List;
import java.util.UUID;

public interface CategoryUseCase {
	Category create(CreateCategoryCommand command);
	Category getById(UUID id);
	List<Category> list();
	void delete(UUID id);
}
