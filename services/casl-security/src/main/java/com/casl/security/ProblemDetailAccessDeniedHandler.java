package com.casl.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

/** 403 en {@code application/problem+json} con {@code code: ACCESS_DENIED}. */
public class ProblemDetailAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	public ProblemDetailAccessDeniedHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException exception) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
				"Insufficient permissions");
		problem.setType(URI.create("https://errors.casl.dev/access-denied"));
		problem.setTitle("Access denied");
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", "ACCESS_DENIED");
		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
