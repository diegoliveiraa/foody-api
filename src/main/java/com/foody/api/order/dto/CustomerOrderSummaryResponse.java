package com.foody.api.order.dto;

import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderItem;
import com.foody.api.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CustomerOrderSummaryResponse(UUID id, long number, OrderStatus status, Instant placedAt, BigDecimal total,
		boolean live, List<OrderItemSummary> items, @Nullable CancellationRequestStatus cancellationRequestStatus) {

	public static CustomerOrderSummaryResponse of(Order order, List<OrderItem> items,
			@Nullable CancellationRequestStatus cancellationRequestStatus) {
		return new CustomerOrderSummaryResponse(order.getId(), order.getNumber(), order.getStatus(),
				order.getPlacedAt(), order.getTotal(), order.getStatus().isActive(),
				items.stream().map(OrderItemSummary::of).toList(), cancellationRequestStatus);
	}

}
