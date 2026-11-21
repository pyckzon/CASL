package com.casl.users.adapter.in.web.error;

import com.casl.users.domain.exception.EmailAlreadyExistsException;
import com.casl.users.domain.exception.InvalidUserException;
import com.casl.users.domain.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Traduce excepciones a {@code application/problem+json} (RFC 9457) con un
 * código estable en la propiedad {@code code}. Los detalles técnicos nunca
 * salen al cliente: se registran y se devuelve un mensaje genérico.
 */
@Log4j2
@RestControllerAdvice
public class ApiExceptionHandler {

	private static final String TYPE_BASE = "https://errors.casl.dev/";

	@ExceptionHandler(UserNotFoundException.class)
	ResponseEntity<ProblemDetail> handleNotFound(UserNotFoundException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found", exception.getMessage(), request);
	}

	@ExceptionHandler(EmailAlreadyExistsException.class)
	ResponseEntity<ProblemDetail> handleEmailConflict(EmailAlreadyExistsException exception,
			HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "USER_EMAIL_ALREADY_EXISTS", "Email already registered",
				exception.getMessage(), request);
	}

	@ExceptionHandler(InvalidUserException.class)
	ResponseEntity<ProblemDetail> handleInvalidUser(InvalidUserException exception, HttpServletRequest request) {
		return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		List<ValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
				.sorted(Comparator.comparing(FieldError::getField))
				.map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
				.toList();
		return validationProblem(errors, request);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ProblemDetail> handleParameterValidation(HandlerMethodValidationException exception,
			HttpServletRequest request) {
		List<ValidationError> errors = exception.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new ValidationError(parameterName(result.getMethodParameter().getParameterName()),
								error.getDefaultMessage())))
				.toList();
		return validationProblem(errors, request);
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
			MethodArgumentTypeMismatchException.class })
	ResponseEntity<ProblemDetail> handleMalformedRequest(Exception exception, HttpServletRequest request) {
		String detail = switch (exception) {
			case MethodArgumentTypeMismatchException mismatch -> parameterName(mismatch.getName())
					+ " has an invalid value";
			case MissingServletRequestParameterException missing -> parameterName(missing.getParameterName())
					+ " is required";
			default -> "The request body or parameters are invalid";
		};
		return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Malformed request", detail, request);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException exception, HttpServletRequest request) {
		return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found",
				"No resource exists at this path", request);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request) {
		return problem(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Method not allowed",
				"The HTTP method is not supported for this resource", request);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ProblemDetail> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception,
			HttpServletRequest request) {
		return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", "Unsupported media type",
				"The request content type is not supported; use application/json", request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Unexpected error handling {} {}", request.getMethod(), request.getRequestURI(), exception);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error",
				"An unexpected error occurred", request);
	}

	private static String parameterName(String name) {
		return (name == null || name.isBlank()) ? "parameter" : name;
	}

	private static ResponseEntity<ProblemDetail> validationProblem(List<ValidationError> errors,
			HttpServletRequest request) {
		ProblemDetail detail = build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed",
				"One or more fields are invalid", request);
		detail.setProperty("errors", errors);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(detail);
	}

	private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String title, String detail,
			HttpServletRequest request) {
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(build(status, code, title, detail, request));
	}

	private static ProblemDetail build(HttpStatus status, String code, String title, String detail,
			HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create(TYPE_BASE + code.toLowerCase(Locale.ROOT).replace('_', '-')));
		problem.setTitle(title);
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", code);
		return problem;
	}
}
