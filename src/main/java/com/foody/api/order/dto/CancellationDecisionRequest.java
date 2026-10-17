package com.foody.api.order.dto;

import com.foody.api.order.OrderConstants;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

public record CancellationDecisionRequest(
		@Nullable @Size(max = OrderConstants.CANCELLATION_NOTE_MAX_LENGTH) String note) {

	public CancellationDecisionRequest {
		note = StringUtils.hasText(note) ? StringUtils.trimWhitespace(note) : null;
	}

}
