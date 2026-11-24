package com.casl.security.userstatus;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class UserStatusEnforcementFilterTest {

	private StubUserStatusVerifier delegate;
	private UserStatusEnforcementFilter filter;

	private final ObjectMapper objectMapper = JsonMapper.builder().build();

	@BeforeEach
	void setUp() {
		delegate = new StubUserStatusVerifier();
		filter = new UserStatusEnforcementFilter(delegate, objectMapper);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void activeUserPassesThrough() throws Exception {
		delegate.status = UserStatus.ACTIVE;

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart/v1/carts");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		authenticate();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(delegate.invocations.get()).isEqualTo(1);
	}

	@Test
	void inactiveUserGetsForbiddenProblemJson() throws Exception {
		delegate.status = UserStatus.INACTIVE;

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart/v1/carts");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		authenticate();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNull();
		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		assertThat(objectMapper.readTree(response.getContentAsString()).at("/properties/code").asString())
				.isEqualTo("USER_INACTIVE");
	}

	@Test
	void userStatusUnavailableGetsServiceUnavailableProblemJson() throws Exception {
		delegate.failure = new UserStatusUnavailableException("users down");

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/catalog/v1/products");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		authenticate();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNull();
		assertThat(response.getStatus()).isEqualTo(503);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		assertThat(objectMapper.readTree(response.getContentAsString()).at("/properties/code").asString())
				.isEqualTo("USER_STATUS_UNAVAILABLE");
	}

	@Test
	void serviceTokenIsNotVerified() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		authenticate(serviceToken());

		filter.doFilter(new MockHttpServletRequest("GET", "/api/orders/v1/orders"), response, chain);

		assertThat(chain.getRequest()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(delegate.invocations.get()).isZero();
	}

	@Test
	void meStatusWithTrailingSlashIsExcludedFromVerification() throws Exception {
		delegate.status = UserStatus.INACTIVE;
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();
		authenticate();

		filter.doFilter(new MockHttpServletRequest("GET", "/api/users/v1/users/me/status/"), response, chain);

		assertThat(chain.getRequest()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(delegate.invocations.get()).isZero();
	}

	@Test
	void requestWithoutAuthenticationContinues() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cart/v1/carts");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void nonApiUriIsNotFiltered() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(chain.getRequest()).isNotNull();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	private void authenticate() {
		SecurityContextHolder.getContext()
				.setAuthentication(new JwtAuthenticationToken(jwt(), List.of(new SimpleGrantedAuthority("ROLE_USER"))));
	}

	private void authenticate(JwtAuthenticationToken token) {
		SecurityContextHolder.getContext().setAuthentication(token);
	}

	private JwtAuthenticationToken serviceToken() {
		return new JwtAuthenticationToken(
				Jwt.withTokenValue("token-value").header("alg", "none")
						.claim("sub", "service-account-casl-order-service")
						.claim("preferred_username", "service-account-casl-order-service")
						.claim("client_id", "casl-order-service")
						.build(),
				List.of());
	}

	private Jwt jwt() {
		return Jwt.withTokenValue("token-value").header("alg", "none").claim("sub", "sub-1").build();
	}

	private static final class StubUserStatusVerifier implements UserStatusVerifier {

		private com.casl.security.userstatus.UserStatus status;

		private RuntimeException failure;

		private final AtomicInteger invocations = new AtomicInteger();

		@Override
		public com.casl.security.userstatus.UserStatus verify(org.springframework.security.oauth2.jwt.Jwt jwt) {
			invocations.incrementAndGet();
			if (failure != null) {
				throw failure;
			}
			return status;
		}
	}
}
