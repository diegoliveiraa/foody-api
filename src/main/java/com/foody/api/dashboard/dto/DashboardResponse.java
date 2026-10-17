package com.foody.api.dashboard.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record DashboardResponse(DashboardPeriod period, String timeZone, Instant generatedAt, DashboardOrders orders,
		BigDecimal revenue, @Nullable BigDecimal averageTicket, DashboardOrdersOverTime ordersOverTime,
		List<DashboardTopProduct> topProducts, DashboardActiveOrders activeByStatus,
		DashboardStageMinutes averageStageMinutes, DashboardCancellations cancellations,
		DashboardPendingRequests pendingCancellationRequests) {
}
