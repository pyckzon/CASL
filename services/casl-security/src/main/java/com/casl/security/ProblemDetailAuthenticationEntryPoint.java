package com.casl.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;

/**
 * 401 en {@code application/problem+json} (RFC 9457) con {@code code}
 * estable. Spring Security invoca este punto de entrada tanto desde el filtro
 * de resource server como desde {@code ExceptionTranslationFilter}.
 */
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	public ProblemDetailAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
				"A valid access token is required");
		problem.setType(URI.create("https://errors.casl.dev/unauthorized"));
		problem.setTitle("Authentication required");
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("code", "UNAUTHORIZED");
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}
}
