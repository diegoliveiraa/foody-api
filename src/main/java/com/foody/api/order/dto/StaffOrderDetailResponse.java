package com.foody.api.order.dto;

import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.CancellationRequest;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderItem;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.entity.OrderStatusHistory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record StaffOrderDetailResponse(UUID id, long number, OrderStatus status, Instant placedAt, boolean live,
		OrderCustomer customer, List<OrderItemResponse> items, BigDecimal total,
		DeliveryAddressResponse deliveryAddress, List<StaffOrderHistoryEntry> history,
		@Nullable OrderCancellationResponse cancellation, List<OrderStatus> allowedTransitions, boolean canCancel,
		List<CancellationReason> cancellationReasons, @Nullable StaffCancellationRequestResponse cancellationRequest) {

	public static StaffOrderDetailResponse of(Order order, List<OrderItem> items, List<OrderStatusHistory> history,
			@Nullable CancellationRequest cancellationRequest, StaffOrderActions actions) {
		return new StaffOrderDetailResponse(order.getId(), order.getNumber(), order.getStatus(), order.getPlacedAt(),
				order.getStatus().isActive(), OrderCustomer.of(order.getCustomer()),
				items.stream().map(OrderItemResponse::of).toList(), order.getTotal(),
				DeliveryAddressResponse.of(order.getDeliveryAddress()),
				history.stream().map(StaffOrderHistoryEntry::of).toList(), OrderCancellationResponse.of(order),
				actions.allowedTransitions(), actions.canCancel(), actions.cancellationReasons(),
				StaffCancellationRequestResponse.of(cancellationRequest));
	}

}
