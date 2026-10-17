package com.foody.api.order.dto;

import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderItem;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.entity.OrderStatusHistory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CustomerOrderDetailResponse(UUID id, long number, OrderStatus status, Instant placedAt, boolean live,
		List<OrderItemResponse> items, BigDecimal total, DeliveryAddressResponse deliveryAddress,
		List<CustomerOrderHistoryEntry> history, @Nullable OrderCancellationResponse cancellation) {

	public static CustomerOrderDetailResponse of(Order order, List<OrderItem> items, List<OrderStatusHistory> history) {
		return new CustomerOrderDetailResponse(order.getId(), order.getNumber(), order.getStatus(), order.getPlacedAt(),
				order.getStatus().isActive(), items.stream().map(OrderItemResponse::of).toList(), order.getTotal(),
				DeliveryAddressResponse.of(order.getDeliveryAddress()),
				history.stream().map(CustomerOrderHistoryEntry::of).toList(), OrderCancellationResponse.of(order));
	}

}
