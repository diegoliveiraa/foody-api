package com.foody.seed.order;

import com.foody.api.order.entity.OrderStatus;
import java.time.Instant;

public record OrderPlan(Instant placedAt, OrderStatus outcome, boolean forFixedCustomer) {
}
