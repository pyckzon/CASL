package com.casl.gateway.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class UserContextFilterTest {

	private final UserContextFilter filter = new UserContextFilter();

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void propagatesJwtSubjectAsUserIdHeader() throws Exception {
		authenticateAs("user-123");

		assertThat(forwardedHeader(new MockHttpServletRequest())).isEqualTo("user-123");
	}

	@Test
	void overridesClientSuppliedHeaderToPreventSpoofing() throws Exception {
		authenticateAs("user-123");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(UserContextFilter.USER_ID_HEADER, "attacker");

		assertThat(forwardedHeader(request)).isEqualTo("user-123");
	}

	@Test
	void leavesRequestUntouchedWithoutAuthentication() throws Exception {
		assertThat(forwardedHeader(new MockHttpServletRequest())).isNull();
	}

	private void authenticateAs(String subject) {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject(subject)
				.claim("sub", subject)
				.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
	}

	private String forwardedHeader(MockHttpServletRequest request) throws Exception {
		AtomicReference<String> forwarded = new AtomicReference<>();
		filter.doFilter(request, new MockHttpServletResponse(), (req, res) ->
				forwarded.set(((HttpServletRequest) req).getHeader(UserContextFilter.USER_ID_HEADER)));
		return forwarded.get();
	}
}
