package com.casl.users.domain.model;

import com.casl.users.domain.exception.InvalidUserException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

	private static final UUID ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000001");
	private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");

	@Test
	void trimsNames() {
		User user = user("  Ana ", "  Pérez  ");

		assertThat(user.firstName()).isEqualTo("Ana");
		assertThat(user.lastName()).isEqualTo("Pérez");
	}

	@Test
	void rejectsBlankNames() {
		assertThatThrownBy(() -> user(" ", "Pérez"))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("firstName");
	}

	@Test
	void rejectsNamesLongerThanContract() {
		assertThatThrownBy(() -> user("a".repeat(101), "Pérez"))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("100");
	}

	@Test
	void updateKeepsIdentityAndCreationTime() {
		Instant updatedAt = CREATED.plusSeconds(60);
		User user = user("Ana", "Pérez");

		User updated = user.update(Email.of("nueva@example.com"), "Ana María", "Gómez", null,
				UserStatus.INACTIVE, updatedAt);

		assertThat(updated.id()).isEqualTo(ID);
		assertThat(updated.createdAt()).isEqualTo(CREATED);
		assertThat(updated.updatedAt()).isEqualTo(updatedAt);
		assertThat(updated.status()).isEqualTo(UserStatus.INACTIVE);
		assertThat(updated.phone().isPresent()).isFalse();
	}

	private static User user(String firstName, String lastName) {
		return new User(ID, Email.of("ana@example.com"), firstName, lastName, null, UserStatus.ACTIVE, CREATED,
				CREATED);
	}
}
