package com.foody.api.order.dto;

import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.entity.OrderStatusHistory;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record StaffOrderHistoryEntry(@Nullable OrderStatus fromStatus, OrderStatus toStatus, Instant occurredAt,
		ActorSide actorSide, String actorName, @Nullable CancellationReason reason, @Nullable String note) {

	public static StaffOrderHistoryEntry of(OrderStatusHistory entry) {
		return new StaffOrderHistoryEntry(entry.getFromStatus(), entry.getToStatus(), entry.getOccurredAt(),
				entry.getActorSide(), entry.getActor().getName(), entry.getReason(), entry.getNote());
	}

}
