package com.casl.catalog.adapter.in.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
		@NotBlank @Size(max = 100) String name,
		@Size(max = 500) String description) {
}
