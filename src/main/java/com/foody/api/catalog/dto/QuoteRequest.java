package com.foody.api.catalog.dto;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.validation.DistinctProducts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record QuoteRequest(@NotNull @Size(min = CatalogConstants.CART_MIN_ITEMS,
		max = CatalogConstants.CART_MAX_ITEMS) @DistinctProducts List<@NotNull @Valid CartItem> items) {
}
