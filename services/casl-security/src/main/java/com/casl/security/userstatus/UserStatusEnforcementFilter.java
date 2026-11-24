package com.casl.security.userstatus;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.casl.security.ServiceToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Aplica el estado del usuario a todos los endpoints protegidos ({@code /api/**}).
 *
 * <p>Fail-closed: {@code INACTIVE} → {@code 403 USER_INACTIVE}; la verificación
 * no disponible → {@code 503 USER_STATUS_UNAVAILABLE}. No existe fail-open.
 */
public class UserStatusEnforcementFilter extends OncePerRequestFilter {

	private static final String API_PREFIX = "/api/";

	private static final List<String> EXCLUDED_URIS = List.of(
			"/api/users/v1/users/me",
			"/api/users/v1/users/me/status");

	private final UserStatusVerifier userStatusVerifier;
	private final ObjectMapper objectMapper;

	public UserStatusEnforcementFilter(UserStatusVerifier userStatusVerifier, ObjectMapper objectMapper) {
		this.userStatusVerifier = userStatusVerifier;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (!uri.startsWith(API_PREFIX)) {
			return true;
		}
		String normalized = (uri.length() > 1 && uri.endsWith("/")) ? uri.substring(0, uri.length() - 1) : uri;
		return EXCLUDED_URIS.contains(normalized);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken)) {
			filterChain.doFilter(request, response);
			return;
		}
		Jwt jwt = (Jwt) authentication.getPrincipal();
		if (ServiceToken.isService(jwt)) {
			filterChain.doFilter(request, response);
			return;
		}
		try {
			UserStatus status = userStatusVerifier.verify(jwt);
			if (status == UserStatus.INACTIVE) {
				writeProblem(response, HttpStatus.FORBIDDEN, "USER_INACTIVE",
						"https://errors.casl.dev/user-inactive", "User inactive",
						"The user is disabled and cannot operate", request.getRequestURI());
				return;
			}
		}
		catch (UserStatusUnavailableException exception) {
			writeProblem(response, HttpStatus.SERVICE_UNAVAILABLE, "USER_STATUS_UNAVAILABLE",
					"https://errors.casl.dev/user-status-unavailable", "User status unavailable",
					"The user status could not be verified", request.getRequestURI());
			return;
		}
		filterChain.doFilter(request, response);
	}

	private void writeProblem(HttpServletResponse response, HttpStatus status, String code, String type, String title,
			String detail, String instance) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create(type));
		problem.setTitle(title);
		setInstanceSafely(problem, instance);
		problem.setProperty("code", code);
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problem);
	}

	private void setInstanceSafely(ProblemDetail problem, String instance) {
		try {
			problem.setInstance(URI.create(instance));
		}
		catch (IllegalArgumentException exception) {
			// La instancia es opcional en RFC 9457; no debe romper la respuesta.
		}
	}
}
