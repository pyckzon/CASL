package com.casl.users.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResultTest {

	@Test
	void computesTotalPages() {
		assertThat(new PageResult<>(List.of(), 0, 20, 0).totalPages()).isZero();
		assertThat(new PageResult<>(List.of(), 0, 20, 1).totalPages()).isEqualTo(1);
		assertThat(new PageResult<>(List.of(), 0, 20, 41).totalPages()).isEqualTo(3);
	}

	@Test
	void copiesContentToRemainImmutable() {
		List<String> source = new java.util.ArrayList<>(List.of("a"));

		PageResult<String> page = new PageResult<>(source, 0, 10, 1);
		source.add("b");

		assertThat(page.content()).containsExactly("a");
	}

	@Test
	void rejectsInvalidMetadata() {
		assertThatThrownBy(() -> new PageResult<>(List.of(), -1, 10, 0))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
