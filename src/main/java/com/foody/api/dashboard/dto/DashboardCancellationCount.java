package com.foody.api.dashboard.dto;

import com.foody.api.order.dto.CancellationCount;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;

public record DashboardCancellationCount(CancellationReason reason, ActorSide cancelledBy, long count) {

	public static DashboardCancellationCount of(CancellationCount cancellations) {
		return new DashboardCancellationCount(cancellations.reason(), cancellations.cancelledBy(),
				cancellations.count());
	}

}
