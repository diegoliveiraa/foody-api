package com.foody.api.order.dto;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.dto.CartItem;
import com.foody.api.catalog.validation.DistinctProducts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PlaceOrderRequest(
		@NotNull @Size(min = CatalogConstants.CART_MIN_ITEMS,
				max = CatalogConstants.CART_MAX_ITEMS) @DistinctProducts List<@NotNull @Valid CartItem> items,
		@NotNull @Valid DeliveryAddressRequest deliveryAddress) {
}
