package com.foody.api.shared.pagination;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class SortGuard {

	private SortGuard() {
	}

	public static Pageable requireAllowed(Pageable pageable, Set<String> allowedProperties) {
		List<String> rejected = pageable.getSort().stream()
				.map(Sort.Order::getProperty)
				.filter(property -> !allowedProperties.contains(property))
				.toList();
		if (rejected.isEmpty()) {
			return pageable;
		}
		throw new InvalidSortException(String.join(PaginationConstants.LIST_DELIMITER, rejected));
	}

}
