package com.casl.catalog.domain.model;

import com.casl.catalog.domain.exception.InvalidProductException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkuTest {

	@Test
	void normalizesToUpperCase() {
		assertThat(Sku.of("  abc-123  ").value()).isEqualTo("ABC-123");
	}

	@Test
	void rejectsBlank() {
		assertThatThrownBy(() -> Sku.of(" ")).isInstanceOf(InvalidProductException.class);
	}

	@Test
	void rejectsTooLong() {
		assertThatThrownBy(() -> Sku.of("A".repeat(65))).isInstanceOf(InvalidProductException.class);
	}

	@Test
	void rejectsInvalidCharacters() {
		assertThatThrownBy(() -> Sku.of("ABC 123")).isInstanceOf(InvalidProductException.class);
	}
}

class MoneyTest {

	@Test
	void normalizesScale() {
		assertThat(Money.of(new BigDecimal("10"), "usd").amount()).isEqualByComparingTo("10.00");
		assertThat(Money.of(new BigDecimal("10"), "usd").currency().getCurrencyCode()).isEqualTo("USD");
	}

	@Test
	void rejectsNegative() {
		assertThatThrownBy(() -> Money.of(new BigDecimal("-1"), "USD"))
				.isInstanceOf(InvalidProductException.class);
	}

	@Test
	void multiplies() {
		assertThat(Money.of(new BigDecimal("5.00"), "USD").multiply(3).amount())
				.isEqualByComparingTo("15.00");
	}
}
