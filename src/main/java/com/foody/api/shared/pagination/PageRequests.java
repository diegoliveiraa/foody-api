package com.foody.api.shared.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequests {

	private PageRequests() {
	}

	public static Pageable sortedOrDefault(Pageable pageable, Sort defaultSort) {
		Sort sort = pageable.getSortOr(defaultSort).and(PaginationConstants.TIEBREAKER_SORT);
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}

}
