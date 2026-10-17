package com.foody.api.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PageRequestsTest {

	private static final Sort DEFAULT_SORT = Sort.by("name");

	@Test
	void sortsAnUnsortedPageByTheDefaultThenById() {
		Pageable pageable = PageRequest.of(2, 25);

		Pageable result = PageRequests.sortedOrDefault(pageable, DEFAULT_SORT);

		assertThat(result.getPageNumber()).isEqualTo(2);
		assertThat(result.getPageSize()).isEqualTo(25);
		assertThat(result.getSort()).containsExactly(Sort.Order.asc("name"), Sort.Order.asc("id"));
	}

	@Test
	void keepsTheRequestedSortAndBreaksTiesById() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("price")));

		Pageable result = PageRequests.sortedOrDefault(pageable, DEFAULT_SORT);

		assertThat(result.getSort()).containsExactly(Sort.Order.desc("price"), Sort.Order.asc("id"));
	}

}
