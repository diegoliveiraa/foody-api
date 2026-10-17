package com.foody.api.order.dto;

import java.time.Instant;
import java.util.UUID;

public interface PendingCancellationRow {

	UUID getOrderId();

	long getNumber();

	Instant getRequestedAt();

}
