package com.casl.users.domain.model;

import com.casl.users.domain.exception.InvalidUserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

	@Test
	void normalizesWhitespaceAndCase() {
		assertThat(Email.of("  Ana.Perez@Example.COM ").value()).isEqualTo("ana.perez@example.com");
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   ", "ana", "ana@", "@example.com", "ana@example", "ana @example.com" })
	void rejectsInvalidAddresses(String value) {
		assertThatThrownBy(() -> Email.of(value))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("email");
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> Email.of(null)).isInstanceOf(InvalidUserException.class);
	}

	@Test
	void equalityUsesNormalizedValue() {
		assertThat(Email.of("ANA@EXAMPLE.COM")).isEqualTo(Email.of("ana@example.com"));
	}
}
