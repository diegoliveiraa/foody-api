package com.foody.api.dashboard.util;

import com.foody.api.dashboard.DashboardConstants;
import com.foody.api.shared.money.MoneyConstants;
import java.math.BigDecimal;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class DashboardMathUtils {

	private DashboardMathUtils() {
	}

	public static @Nullable BigDecimal percentOf(long part, long whole) {
		if (whole == 0) {
			return null;
		}
		return BigDecimal.valueOf(part)
			.multiply(DashboardConstants.PERCENT)
			.divide(BigDecimal.valueOf(whole), DashboardConstants.PERCENT_SCALE, DashboardConstants.PERCENT_ROUNDING);
	}

	public static @Nullable Long roundedMinutes(@Nullable BigDecimal seconds) {
		return Optional.ofNullable(seconds)
			.map(value -> value
				.divide(DashboardConstants.SECONDS_PER_MINUTE, DashboardConstants.MINUTES_SCALE,
						DashboardConstants.MINUTES_ROUNDING)
				.longValueExact())
			.orElse(null);
	}

	public static @Nullable BigDecimal averageAmount(BigDecimal total, long count) {
		if (count == 0) {
			return null;
		}
		return total.divide(BigDecimal.valueOf(count), MoneyConstants.SCALE, MoneyConstants.ROUNDING);
	}

}
