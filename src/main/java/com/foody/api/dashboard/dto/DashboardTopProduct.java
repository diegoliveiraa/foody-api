package com.foody.api.dashboard.dto;

import com.foody.api.order.dto.ProductSales;
import java.math.BigDecimal;
import java.util.UUID;

public record DashboardTopProduct(UUID productId, String name, long quantity, BigDecimal revenue) {

	public static DashboardTopProduct of(ProductSales sales) {
		return new DashboardTopProduct(sales.productId(), sales.name(), sales.quantity(), sales.revenue());
	}

}
