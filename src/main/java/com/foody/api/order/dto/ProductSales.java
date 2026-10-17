package com.foody.api.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSales(UUID productId, String name, long quantity, BigDecimal revenue) {

	public static ProductSales of(ProductSalesRow row) {
		return new ProductSales(row.getProductId(), row.getName(), row.getQuantity(), row.getRevenue());
	}

}
