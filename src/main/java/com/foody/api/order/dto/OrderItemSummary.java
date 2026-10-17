package com.foody.api.order.dto;

import com.foody.api.order.entity.OrderItem;

public record OrderItemSummary(String productName, int quantity) {

	public static OrderItemSummary of(OrderItem item) {
		return new OrderItemSummary(item.getProductName(), item.getQuantity());
	}

}
