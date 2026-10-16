package com.foody.api.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class SortGuardTest {

	private static final Set<String> ALLOWED = Set.of("name", "createdAt");

	@Test
	void keepsThePageableWhenEverySortPropertyIsAllowed() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("name", "createdAt"));

		Pageable result = SortGuard.requireAllowed(pageable, ALLOWED);

		assertThat(result).isSameAs(pageable);
	}

	@Test
	void keepsAnUnsortedPageable() {
		Pageable pageable = PageRequest.of(0, 10);

		Pageable result = SortGuard.requireAllowed(pageable, ALLOWED);

		assertThat(result).isSameAs(pageable);
	}

	@Test
	void rejectsAPageableSortedByAPropertyOutsideTheAllowlistNamingIt() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("name", "passwordHash"));

		assertThatThrownBy(() -> SortGuard.requireAllowed(pageable, ALLOWED))
				.isInstanceOf(InvalidSortException.class)
				.hasMessageEndingWith("passwordHash");
	}

}
