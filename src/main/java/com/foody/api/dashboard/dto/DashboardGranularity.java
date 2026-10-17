package com.foody.api.dashboard.dto;

import java.time.temporal.ChronoUnit;

public enum DashboardGranularity {

	HOUR(ChronoUnit.HOURS), DAY(ChronoUnit.DAYS);

	private final ChronoUnit unit;

	DashboardGranularity(ChronoUnit unit) {
		this.unit = unit;
	}

	public ChronoUnit unit() {
		return unit;
	}

}
