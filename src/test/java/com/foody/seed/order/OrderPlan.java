package com.foody.seed.order;

import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.order.entity.OrderStatus;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record OrderPlan(Instant placedAt, OrderStatus outcome, boolean forFixedCustomer,
		@Nullable CancellationRequestStatus cancellationRequest) {
}
