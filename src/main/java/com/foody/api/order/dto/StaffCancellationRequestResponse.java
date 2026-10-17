package com.foody.api.order.dto;

import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.CancellationRequest;
import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.user.entity.User;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record StaffCancellationRequestResponse(CancellationRequestStatus status, CancellationReason reason,
		@Nullable String note, Instant requestedAt, @Nullable Instant decidedAt, @Nullable String decisionNote,
		@Nullable String decidedByName) {

	public static @Nullable StaffCancellationRequestResponse of(@Nullable CancellationRequest request) {
		if (request == null) {
			return null;
		}
		String decidedByName = Optional.ofNullable(request.getDecidedBy()).map(User::getName).orElse(null);
		return new StaffCancellationRequestResponse(request.getStatus(), request.getReason(), request.getNote(),
				request.getRequestedAt(), request.getDecidedAt(), request.getDecisionNote(), decidedByName);
	}

}
