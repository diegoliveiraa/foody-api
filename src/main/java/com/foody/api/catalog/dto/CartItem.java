package com.foody.api.catalog.dto;

import com.foody.api.catalog.CatalogConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CartItem(@NotNull UUID productId,
		@NotNull @Min(CatalogConstants.QUANTITY_MIN) @Max(CatalogConstants.QUANTITY_MAX) Integer quantity) {
}
