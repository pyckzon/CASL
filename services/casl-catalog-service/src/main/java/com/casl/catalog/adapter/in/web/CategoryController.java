package com.casl.catalog.adapter.in.web;

import com.casl.catalog.application.command.CreateCategoryCommand;
import com.casl.catalog.application.port.in.CategoryUseCase;
import com.casl.catalog.domain.model.Category;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalog/v1/categories")
public class CategoryController {

	private static final String BASE = "/api/catalog/v1/categories/";

	private final CategoryUseCase useCase;

	public CategoryController(CategoryUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
		Category created = useCase.create(new CreateCategoryCommand(request.name(), request.description()));
		return ResponseEntity.created(URI.create(BASE + created.id()))
				.body(new CategoryResponse(created.id(), created.name(), created.description(), created.createdAt(),
						created.updatedAt()));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	public CategoryResponse get(@PathVariable UUID id) {
		Category category = useCase.getById(id);
		return new CategoryResponse(category.id(), category.name(), category.description(), category.createdAt(),
				category.updatedAt());
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('USER','ADMIN')")
	public List<CategoryResponse> list() {
		return useCase.list().stream()
				.map(c -> new CategoryResponse(c.id(), c.name(), c.description(), c.createdAt(), c.updatedAt()))
				.toList();
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		useCase.delete(id);
		return ResponseEntity.noContent().build();
	}
}
