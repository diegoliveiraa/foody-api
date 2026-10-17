package com.foody.api.order.dto;

import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.Order;
import org.jspecify.annotations.Nullable;

public record OrderCancellationResponse(ActorSide cancelledBy, CancellationReason reason, @Nullable String note) {

	public static @Nullable OrderCancellationResponse of(Order order) {
		ActorSide cancelledBy = order.getCancelledBy();
		CancellationReason reason = order.getCancelReason();
		if (cancelledBy == null || reason == null) {
			return null;
		}
		return new OrderCancellationResponse(cancelledBy, reason, order.getCancelNote());
	}

}
