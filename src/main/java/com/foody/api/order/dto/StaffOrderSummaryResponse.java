package com.foody.api.order.dto;

import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderItem;
import com.foody.api.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StaffOrderSummaryResponse(UUID id, long number, OrderStatus status, Instant placedAt, BigDecimal total,
		boolean live, String customerName, List<OrderItemSummary> items, boolean hasPendingCancellationRequest,
		List<OrderStatus> allowedTransitions, boolean canCancel, List<CancellationReason> cancellationReasons) {

	public static StaffOrderSummaryResponse of(Order order, List<OrderItem> items,
			boolean hasPendingCancellationRequest, StaffOrderActions actions) {
		return new StaffOrderSummaryResponse(order.getId(), order.getNumber(), order.getStatus(), order.getPlacedAt(),
				order.getTotal(), order.getStatus().isActive(), order.getCustomer().getName(),
				items.stream().map(OrderItemSummary::of).toList(), hasPendingCancellationRequest,
				actions.allowedTransitions(), actions.canCancel(), actions.cancellationReasons());
	}

}
