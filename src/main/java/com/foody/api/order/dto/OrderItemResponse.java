package com.foody.api.order.dto;

import com.foody.api.order.entity.OrderItem;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(UUID productId, String productName, BigDecimal unitPrice, int quantity,
		BigDecimal subtotal) {

	public static OrderItemResponse of(OrderItem item) {
		return new OrderItemResponse(item.getProductId(), item.getProductName(), item.getUnitPrice(),
				item.getQuantity(), item.getSubtotal());
	}

}
