package com.foody.api.order.dto;

import com.foody.api.order.entity.CancellationRequestStatus;
import java.util.UUID;

public record CancellationRequestState(UUID orderId, CancellationRequestStatus status) {
}
