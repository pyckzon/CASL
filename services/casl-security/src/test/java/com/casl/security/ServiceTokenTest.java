package com.casl.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceTokenTest {

	private static final String ALLOWED_CLIENT = "casl-order-service";

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void recognizesServiceAccountsByPreferredUsername() {
		assertThat(ServiceToken.isService(serviceJwt(ALLOWED_CLIENT))).isTrue();
		assertThat(ServiceToken.isService(userJwt())).isFalse();
		assertThat(ServiceToken.isService(null)).isFalse();
	}

	@Test
	void adminCanManage() {
		authenticate("ROLE_ADMIN");

		assertThatCode(() -> ServiceToken.requireManager(userJwt(), ALLOWED_CLIENT))
				.doesNotThrowAnyException();
	}

	@Test
	void plainUserCannotManage() {
		authenticate("ROLE_USER");

		assertThatThrownBy(() -> ServiceToken.requireManager(userJwt(), ALLOWED_CLIENT))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void permittedServiceAccountCanManageEvenWithoutRole() {
		authenticate("ROLE_USER");

		assertThatCode(() -> ServiceToken.requireManager(serviceJwt(ALLOWED_CLIENT), ALLOWED_CLIENT))
				.doesNotThrowAnyException();
	}

	@Test
	void anotherServiceAccountCannotManage() {
		authenticate("ROLE_USER");

		assertThatThrownBy(() -> ServiceToken.requireManager(serviceJwt("other-client"), ALLOWED_CLIENT))
				.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void userTokenWithSpoofedClientIdIsNotAService() {
		authenticate("ROLE_USER");
		Jwt spoofed = Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject(UUID.randomUUID().toString())
				.claim("client_id", ALLOWED_CLIENT)
				.build();

		assertThatThrownBy(() -> ServiceToken.requireManager(spoofed, ALLOWED_CLIENT))
				.isInstanceOf(AccessDeniedException.class);
	}

	private static void authenticate(String role) {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
				"principal", "credentials", List.of(new SimpleGrantedAuthority(role))));
	}

	private static Jwt userJwt() {
		return Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject(UUID.randomUUID().toString())
				.claim("preferred_username", "alice")
				.build();
	}

	private static Jwt serviceJwt(String clientId) {
		return Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject(UUID.randomUUID().toString())
				.claim("preferred_username", "service-account-" + clientId)
				.claim("client_id", clientId)
				.build();
	}
}
