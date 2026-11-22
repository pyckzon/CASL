package com.casl.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailHandlersTest {

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	@Test
	void unauthorizedResponseIsProblemJson() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart/v1/carts");
		MockHttpServletResponse response = new MockHttpServletResponse();

		new ProblemDetailAuthenticationEntryPoint(objectMapper)
				.commence(request, response, new BadCredentialsException("invalid"));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).startsWith("application/problem+json");
		// Un ObjectMapper plano anida las propiedades; el de Spring Boot las aplana,
		// como comprueban los tests de seguridad de los servicios ($.code).
		assertThat(objectMapper.readTree(response.getContentAsString()).at("/properties/code").asString())
				.isEqualTo("UNAUTHORIZED");
	}

	@Test
	void forbiddenResponseIsProblemJson() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart/v1/carts");
		MockHttpServletResponse response = new MockHttpServletResponse();

		new ProblemDetailAccessDeniedHandler(objectMapper)
				.handle(request, response, new AccessDeniedException("denied"));

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentType()).startsWith("application/problem+json");
		assertThat(objectMapper.readTree(response.getContentAsString()).at("/properties/code").asString())
				.isEqualTo("ACCESS_DENIED");
	}
}
