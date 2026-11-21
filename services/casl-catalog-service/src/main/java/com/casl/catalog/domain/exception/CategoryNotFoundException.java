package com.casl.catalog.domain.exception;

public class CategoryNotFoundException extends RuntimeException {
	public CategoryNotFoundException(java.util.UUID id) {
		super("No category with id " + id);
	}
}
