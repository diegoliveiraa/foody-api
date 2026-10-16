package com.foody.api.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

	@Test
	void describesTheFirstPageWithOneBasedNumbering() {
		PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 10), 25);

		PageResponse<String> response = PageResponse.of(page);

		assertThat(response.data()).containsExactly("a", "b");
		assertThat(response.meta()).isEqualTo(new PageMeta(1, 10, 25, 3, true, false));
	}

	@Test
	void describesTheLastPageAsHavingNoNextPage() {
		PageImpl<String> page = new PageImpl<>(List.of("v", "w", "x", "y", "z"), PageRequest.of(2, 10), 25);

		PageResponse<String> response = PageResponse.of(page);

		assertThat(response.meta()).isEqualTo(new PageMeta(3, 10, 25, 3, false, true));
	}

}
