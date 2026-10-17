package com.foody.api.dashboard.dto;

import com.foody.api.order.dto.PendingCancellation;
import java.util.List;

public record DashboardPendingRequests(long count, List<DashboardPendingRequest> items) {

	public static DashboardPendingRequests of(long count, List<PendingCancellation> oldest) {
		return new DashboardPendingRequests(count, oldest.stream().map(DashboardPendingRequest::of).toList());
	}

}
