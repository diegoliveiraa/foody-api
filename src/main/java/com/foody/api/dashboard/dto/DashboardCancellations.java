package com.foody.api.dashboard.dto;

import com.foody.api.dashboard.DashboardConstants;
import com.foody.api.dashboard.util.DashboardMathUtils;
import com.foody.api.order.dto.CancellationCount;
import com.foody.api.order.dto.OrderTotals;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record DashboardCancellations(long count, @Nullable BigDecimal ratePercent,
		List<DashboardCancellationCount> byReason) {

	public static DashboardCancellations of(OrderTotals totals, List<CancellationCount> counts) {
		List<DashboardCancellationCount> byReason = counts.stream()
			.map(DashboardCancellationCount::of)
			.sorted(DashboardConstants.CANCELLATION_ORDER)
			.toList();
		return new DashboardCancellations(totals.cancelled(),
				DashboardMathUtils.percentOf(totals.cancelled(), totals.placed()), byReason);
	}

}
