package com.foody.api.order.dto;

import com.foody.api.order.entity.CancellationReason;
import java.util.List;

public record CustomerOrderActions(boolean canCancel, boolean canRequestCancellation,
		List<CancellationReason> cancellationReasons) {
}
