package com.casl.shipping.adapter.in.web.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.Locale;

@Log4j2
@RestControllerAdvice
public class ApiExceptionHandler {

	private static final String TYPE_BASE = "https://errors.casl.dev/";

	@ExceptionHandler(IllegalArgumentException.class)
	ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "SHIPMENT_NOT_FOUND", "Shipment not found", exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", "One or more fields are invalid", request);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class })
	ResponseEntity<ProblemDetail> handleMalformedRequest(Exception exception, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request", "The request body or parameters are invalid", request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", "No resource exists at this path", request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ProblemDetail> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
		return problem(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed", "The HTTP method is not supported for this resource", request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ProblemDetail> handleUnsupportedMediaType(org.springframework.web.HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
		return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type", "The request content type is not supported; use application/json", request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Unexpected error handling {} {}", request.getMethod(), request.getRequestURI(), exception);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", "An unexpected error occurred", request);
	}

	private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String title, String detail, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create(TYPE_BASE + code.toLowerCase(Locale.ROOT).replace('_', '-')));
		problem.setTitle(title);
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", code);
		org.springframework.http.MediaType contentType = org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
		return ResponseEntity.status(status).contentType(contentType).body(problem);
	}
}
