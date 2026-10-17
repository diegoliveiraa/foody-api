package com.foody.api.order.dto;

import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.OrderStatus;
import java.util.List;

public record StaffOrderActions(List<OrderStatus> allowedTransitions, boolean canCancel,
		List<CancellationReason> cancellationReasons) {
}
