package com.foody.api.order.dto;

import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;

public record CancellationCount(CancellationReason reason, ActorSide cancelledBy, long count) {

	public static CancellationCount of(CancellationCountRow row) {
		return new CancellationCount(row.getReason(), row.getCancelledBy(), row.getOrderCount());
	}

}
