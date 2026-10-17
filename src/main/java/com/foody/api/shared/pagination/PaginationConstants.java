package com.foody.api.shared.pagination;

import com.foody.api.shared.persistence.PersistenceConstants;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;

public final class PaginationConstants {

	public static final int FIRST_PAGE_NUMBER = 1;

	public static final String PAGE_SIZE_PARAMETER = "pageSize";

	public static final String LIST_DELIMITER = ", ";

	public static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 25, 50, 100);

	public static final int DEFAULT_PAGE_SIZE = ALLOWED_PAGE_SIZES.getFirst();

	public static final int MAX_PAGE_SIZE = ALLOWED_PAGE_SIZES.getLast();

	public static final Set<String> ALLOWED_PAGE_SIZE_VALUES = ALLOWED_PAGE_SIZES.stream()
		.map(String::valueOf)
		.collect(Collectors.toUnmodifiableSet());

	public static final String ALLOWED_PAGE_SIZES_TEXT = ALLOWED_PAGE_SIZES.stream()
		.map(String::valueOf)
		.collect(Collectors.joining(LIST_DELIMITER));

	public static final Sort TIEBREAKER_SORT = Sort.by(PersistenceConstants.ID_ATTRIBUTE);

	private PaginationConstants() {
	}

}
