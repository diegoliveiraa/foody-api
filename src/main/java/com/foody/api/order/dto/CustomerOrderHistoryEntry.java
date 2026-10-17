package com.foody.api.order.dto;

import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.entity.OrderStatusHistory;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record CustomerOrderHistoryEntry(@Nullable OrderStatus fromStatus, OrderStatus toStatus, Instant occurredAt,
		ActorSide actorSide, @Nullable CancellationReason reason, @Nullable String note) {

	public static CustomerOrderHistoryEntry of(OrderStatusHistory entry) {
		return new CustomerOrderHistoryEntry(entry.getFromStatus(), entry.getToStatus(), entry.getOccurredAt(),
				entry.getActorSide(), entry.getReason(), entry.getNote());
	}

}
