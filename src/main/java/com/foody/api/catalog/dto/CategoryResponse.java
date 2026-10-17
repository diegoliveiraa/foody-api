package com.foody.api.catalog.dto;

import com.foody.api.catalog.entity.Category;
import com.foody.api.shared.persistence.ActivationStatus;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CategoryResponse(UUID id, String name, int displayOrder, ActivationStatus status, long productCount,
		@Nullable Instant disabledAt) {

	public static CategoryResponse of(Category category, long productCount) {
		return new CategoryResponse(category.getId(), category.getName(), category.getDisplayOrder(),
				ActivationStatus.of(category), productCount, category.getDisabledAt());
	}

}
