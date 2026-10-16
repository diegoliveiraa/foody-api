package com.foody.api.shared.pagination;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(List<T> data, PageMeta meta) {

	public static <T> PageResponse<T> of(Page<T> page) {
		return new PageResponse<>(page.getContent(), PageMeta.of(page));
	}

}
