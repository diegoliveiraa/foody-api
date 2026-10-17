package com.foody.api.catalog.dto;

import com.foody.api.catalog.entity.Product;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record QuotedItem(UUID productId, @Nullable String name, @Nullable BigDecimal unitPrice, int quantity,
		@Nullable BigDecimal subtotal, boolean available) {

	public static QuotedItem missing(CartItem item) {
		return new QuotedItem(item.productId(), null, null, item.quantity(), null, false);
	}

	public static QuotedItem unavailable(CartItem item, Product product) {
		return new QuotedItem(item.productId(), product.getName(), null, item.quantity(), null, false);
	}

	public static QuotedItem available(CartItem item, Product product, BigDecimal subtotal) {
		return new QuotedItem(item.productId(), product.getName(), product.getPrice(), item.quantity(), subtotal, true);
	}

}
