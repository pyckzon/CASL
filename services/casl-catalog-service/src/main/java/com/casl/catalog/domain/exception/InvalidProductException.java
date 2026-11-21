package com.casl.catalog.domain.exception;

public class InvalidProductException extends RuntimeException {
	public InvalidProductException(String message) {
		super(message);
	}
}
