package com.foody.api.catalog.dto;

import com.foody.api.catalog.entity.Category;
import java.util.UUID;

public record ProductCategory(UUID id, String name) {

	public static ProductCategory of(Category category) {
		return new ProductCategory(category.getId(), category.getName());
	}

}
