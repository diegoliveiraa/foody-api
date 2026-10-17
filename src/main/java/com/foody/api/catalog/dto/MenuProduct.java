package com.foody.api.catalog.dto;

import com.foody.api.catalog.entity.Product;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record MenuProduct(UUID id, String name, @Nullable String description, BigDecimal price) {

	public static MenuProduct of(Product product) {
		return new MenuProduct(product.getId(), product.getName(), product.getDescription(), product.getPrice());
	}

}
