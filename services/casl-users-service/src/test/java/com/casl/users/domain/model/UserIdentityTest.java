package com.casl.users.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdentityTest {

	private static final UUID USER_ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000001");

	@Test
	void trimsProviderAndSubject() {
		UserIdentity identity = new UserIdentity(USER_ID, " keycloak ", " subject-1 ");

		assertThat(identity.provider()).isEqualTo("keycloak");
		assertThat(identity.subject()).isEqualTo("subject-1");
	}

	@Test
	void rejectsBlankSubject() {
		assertThatThrownBy(() -> new UserIdentity(USER_ID, UserIdentity.KEYCLOAK, " "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("subject");
	}

	@Test
	void rejectsMissingUserId() {
		assertThatThrownBy(() -> new UserIdentity(null, UserIdentity.KEYCLOAK, "subject-1"))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void rejectsOverlongSubject() {
		assertThatThrownBy(() -> new UserIdentity(USER_ID, UserIdentity.KEYCLOAK, "x".repeat(101)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("subject");
	}
}
