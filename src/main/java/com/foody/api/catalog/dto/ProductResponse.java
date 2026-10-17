package com.foody.api.catalog.dto;

import com.foody.api.catalog.entity.Product;
import com.foody.api.shared.persistence.ActivationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ProductResponse(UUID id, String name, @Nullable String description, BigDecimal price,
		ProductCategory category, ActivationStatus status, Instant createdAt, @Nullable Instant disabledAt) {

	public static ProductResponse of(Product product) {
		return new ProductResponse(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
				ProductCategory.of(product.getCategory()), ActivationStatus.of(product), product.getCreatedAt(),
				product.getDisabledAt());
	}

}
