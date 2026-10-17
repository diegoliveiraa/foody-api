package com.foody.api.catalog.dto;

import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import java.util.List;
import java.util.UUID;

public record MenuCategory(UUID id, String name, List<MenuProduct> products) {

	public static MenuCategory of(Category category, List<Product> products) {
		return new MenuCategory(category.getId(), category.getName(), products.stream().map(MenuProduct::of).toList());
	}

}
