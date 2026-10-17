package com.foody.api.dashboard.dto;

import com.foody.api.dashboard.util.DashboardMathUtils;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record DashboardOrders(long count, long previousCount, @Nullable BigDecimal changePercent) {

	public static DashboardOrders of(long count, long previousCount) {
		return new DashboardOrders(count, previousCount,
				DashboardMathUtils.percentOf(count - previousCount, previousCount));
	}

}
