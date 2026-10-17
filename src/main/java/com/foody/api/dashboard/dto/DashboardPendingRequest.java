package com.foody.api.dashboard.dto;

import com.foody.api.order.dto.PendingCancellation;
import java.time.Instant;
import java.util.UUID;

public record DashboardPendingRequest(UUID orderId, long number, Instant requestedAt) {

	public static DashboardPendingRequest of(PendingCancellation request) {
		return new DashboardPendingRequest(request.orderId(), request.number(), request.requestedAt());
	}

}
