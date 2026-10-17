package com.foody.api.order.dto;

import java.time.Instant;
import java.util.UUID;

public record PendingCancellation(UUID orderId, long number, Instant requestedAt) {

	public static PendingCancellation of(PendingCancellationRow row) {
		return new PendingCancellation(row.getOrderId(), row.getNumber(), row.getRequestedAt());
	}

}
