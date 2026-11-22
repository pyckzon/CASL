package com.casl.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSubjectTest {

	private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Test
	void readsUuidSubject() {
		assertThat(JwtSubject.requireUuid(jwt(USER_ID.toString()))).isEqualTo(USER_ID);
	}

	@Test
	void rejectsMissingTokenOrSubject() {
		assertThatThrownBy(() -> JwtSubject.requireUuid(null))
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
		assertThatThrownBy(() -> JwtSubject.requireUuid(jwt(" ")))
				.isInstanceOf(AuthenticationCredentialsNotFoundException.class);
	}

	@Test
	void rejectsNonUuidSubject() {
		assertThatThrownBy(() -> JwtSubject.requireUuid(jwt("not-a-uuid")))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void ownerCheckAcceptsMatchingSubject() {
		JwtSubject.requireOwner(USER_ID, jwt(USER_ID.toString()));
	}

	@Test
	void ownerCheckRejectsAnotherSubject() {
		assertThatThrownBy(() -> JwtSubject.requireOwner(USER_ID, jwt(UUID.randomUUID().toString())))
				.isInstanceOf(AccessDeniedException.class);
	}

	private static Jwt jwt(String subject) {
		return Jwt.withTokenValue("token")
				.header("alg", "none")
				.subject(subject)
				.build();
	}
}
