package com.casl.users.config.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiMaskerTest {

	@Test
	void masksEmailsAnywhere() {
		assertThat(PiiMasker.mask("User created email=ana@example.com")).isEqualTo("User created email=a***@example.com");
		assertThat(PiiMasker.mask("contacto: juan.perez@sub.example.co.uk")).isEqualTo("contacto: j***@sub.example.co.uk");
	}

	@Test
	void masksLabeledPhonesKeepingLastFourDigits() {
		assertThat(PiiMasker.mask("phone=+57 300 1234567")).isEqualTo("phone=***4567");
		assertThat(PiiMasker.mask("telefono: 3001234567")).isEqualTo("telefono=***4567");
	}

	@Test
	void leavesNonPiiDataUntouched() {
		String message = "id=3f1c2a7e-0000-0000-0000-000000000001 at 2026-01-01T10:00:00Z status=ACTIVE";

		assertThat(PiiMasker.mask(message)).isEqualTo(message);
	}

	@Test
	void handlesNullAndEmpty() {
		assertThat(PiiMasker.mask(null)).isNull();
		assertThat(PiiMasker.mask("")).isEmpty();
	}
}
