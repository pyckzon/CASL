package com.casl.users.domain.model;

import com.casl.users.domain.exception.InvalidUserException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

	@Test
	void blankBecomesNull() {
		assertThat(PhoneNumber.of("   ").value()).isNull();
		assertThat(PhoneNumber.of(null).isPresent()).isFalse();
	}

	@Test
	void trimsValidNumber() {
		PhoneNumber phone = PhoneNumber.of(" +57 300 1234567 ");

		assertThat(phone.value()).isEqualTo("+57 300 1234567");
		assertThat(phone.isPresent()).isTrue();
	}

	@Test
	void rejectsInvalidCharacters() {
		assertThatThrownBy(() -> PhoneNumber.of("call-me-maybe"))
				.isInstanceOf(InvalidUserException.class)
				.hasMessageContaining("phone");
	}

	@Test
	void rejectsTooLongNumber() {
		assertThatThrownBy(() -> PhoneNumber.of("+1234567890123456789012345678901"))
				.isInstanceOf(InvalidUserException.class);
	}
}
