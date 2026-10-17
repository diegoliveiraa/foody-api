package com.foody.api.order.dto;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.validation.CancellationNote;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

@CancellationNote
public record CancelOrderRequest(@NotNull CancellationReason reason,
		@Nullable @Size(max = OrderConstants.CANCELLATION_NOTE_MAX_LENGTH) String note) {

	public CancelOrderRequest {
		note = StringUtils.hasText(note) ? StringUtils.trimWhitespace(note) : null;
	}

}
