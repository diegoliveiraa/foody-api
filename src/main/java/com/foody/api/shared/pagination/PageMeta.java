package com.foody.api.shared.pagination;

import org.springframework.data.domain.Page;

public record PageMeta(int page, int pageSize, long totalItems, int totalPages, boolean hasNext,
		boolean hasPrevious) {

	public static PageMeta of(Page<?> page) {
		return new PageMeta(page.getNumber() + PaginationConstants.FIRST_PAGE_NUMBER, page.getSize(),
				page.getTotalElements(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
	}

}
