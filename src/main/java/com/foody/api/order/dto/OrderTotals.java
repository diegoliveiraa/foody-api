package com.foody.api.order.dto;

import java.math.BigDecimal;

public record OrderTotals(long placed, long delivered, BigDecimal revenue, long cancelled) {

	public static OrderTotals of(OrderTotalsRow row) {
		return new OrderTotals(row.getPlaced(), row.getDelivered(), row.getRevenue(), row.getCancelled());
	}

}
