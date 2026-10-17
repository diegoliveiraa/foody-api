package com.foody.api.dashboard;

import com.foody.api.dashboard.dto.DashboardCancellationCount;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

public final class DashboardConstants {

	public static final String DASHBOARD_PATH = "/dashboard";

	public static final String DEFAULT_PERIOD = "TODAY";

	public static final int TOP_PRODUCTS_LIMIT = 5;

	public static final int PENDING_REQUESTS_LIMIT = 10;

	public static final long BUCKET_STEP = 1;

	public static final long NO_ORDERS = 0;

	public static final BigDecimal PERCENT = BigDecimal.valueOf(100);

	public static final int PERCENT_SCALE = 1;

	public static final RoundingMode PERCENT_ROUNDING = RoundingMode.HALF_UP;

	public static final BigDecimal SECONDS_PER_MINUTE = BigDecimal
		.valueOf(ChronoUnit.MINUTES.getDuration().toSeconds());

	public static final int MINUTES_SCALE = 0;

	public static final RoundingMode MINUTES_ROUNDING = RoundingMode.HALF_UP;

	public static final Comparator<DashboardCancellationCount> CANCELLATION_ORDER = Comparator
		.comparingLong(DashboardCancellationCount::count)
		.reversed()
		.thenComparing(DashboardCancellationCount::reason)
		.thenComparing(DashboardCancellationCount::cancelledBy);

	private DashboardConstants() {
	}

}
