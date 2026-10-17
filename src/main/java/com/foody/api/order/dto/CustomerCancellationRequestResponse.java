package com.foody.api.order.dto;

import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.CancellationRequest;
import com.foody.api.order.entity.CancellationRequestStatus;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record CustomerCancellationRequestResponse(CancellationRequestStatus status, CancellationReason reason,
		@Nullable String note, Instant requestedAt, @Nullable Instant decidedAt, @Nullable String decisionNote) {

	public static @Nullable CustomerCancellationRequestResponse of(@Nullable CancellationRequest request) {
		if (request == null) {
			return null;
		}
		return new CustomerCancellationRequestResponse(request.getStatus(), request.getReason(), request.getNote(),
				request.getRequestedAt(), request.getDecidedAt(), request.getDecisionNote());
	}

}
