package com.foody.api.dashboard.service;

import com.foody.api.dashboard.DashboardConstants;
import com.foody.api.dashboard.dto.DashboardActiveOrders;
import com.foody.api.dashboard.dto.DashboardCancellations;
import com.foody.api.dashboard.dto.DashboardOrders;
import com.foody.api.dashboard.dto.DashboardOrdersOverTime;
import com.foody.api.dashboard.dto.DashboardPendingRequests;
import com.foody.api.dashboard.dto.DashboardPeriod;
import com.foody.api.dashboard.dto.DashboardResponse;
import com.foody.api.dashboard.dto.DashboardStageMinutes;
import com.foody.api.dashboard.dto.DashboardTopProduct;
import com.foody.api.dashboard.dto.DashboardWindow;
import com.foody.api.dashboard.util.DashboardMathUtils;
import com.foody.api.order.dto.OrderTotals;
import com.foody.api.order.service.OrderMetricsService;
import com.foody.api.shared.money.MoneyConstants;
import com.foody.api.shared.restaurant.RestaurantProperties;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

	private final OrderMetricsService metrics;

	private final RestaurantProperties restaurant;

	private final Clock clock;

	public DashboardService(OrderMetricsService metrics, RestaurantProperties restaurant, Clock clock) {
		this.metrics = metrics;
		this.restaurant = restaurant;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public DashboardResponse overview(DashboardPeriod period) {
		DashboardWindow window = DashboardWindow.of(period, restaurant, clock);
		OrderTotals totals = metrics.totalsBetween(window.start(), window.end());
		long previousCount = metrics.countPlacedBetween(window.previousStart(), window.previousEnd());
		return new DashboardResponse(period, window.zone().getId(), window.end(),
				DashboardOrders.of(totals.placed(), previousCount),
				totals.revenue().setScale(MoneyConstants.SCALE, MoneyConstants.ROUNDING),
				DashboardMathUtils.averageAmount(totals.revenue(), totals.delivered()), ordersOverTime(window),
				topProducts(window), DashboardActiveOrders.of(metrics.countActiveByStatus()),
				DashboardStageMinutes.of(metrics.averageStageDurations(window.start(), window.end())),
				DashboardCancellations.of(totals, metrics.cancellationsBetween(window.start(), window.end())),
				pendingRequests());
	}

	private DashboardOrdersOverTime ordersOverTime(DashboardWindow window) {
		return DashboardOrdersOverTime.of(window, metrics.placedPerBucket(window.start(), window.end(),
				window.period().granularity().unit(), window.zone()));
	}

	private List<DashboardTopProduct> topProducts(DashboardWindow window) {
		return metrics.topDeliveredProducts(window.start(), window.end(), DashboardConstants.TOP_PRODUCTS_LIMIT)
			.stream()
			.map(DashboardTopProduct::of)
			.toList();
	}

	private DashboardPendingRequests pendingRequests() {
		return DashboardPendingRequests.of(metrics.countPendingCancellationRequests(),
				metrics.oldestPendingCancellationRequests(DashboardConstants.PENDING_REQUESTS_LIMIT));
	}

}
