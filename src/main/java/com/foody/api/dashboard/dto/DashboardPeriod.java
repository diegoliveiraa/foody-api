package com.foody.api.dashboard.dto;

public enum DashboardPeriod {

	TODAY(1, DashboardGranularity.HOUR), LAST_7_DAYS(7, DashboardGranularity.DAY),
	LAST_30_DAYS(30, DashboardGranularity.DAY);

	private final int days;

	private final DashboardGranularity granularity;

	DashboardPeriod(int days, DashboardGranularity granularity) {
		this.days = days;
		this.granularity = granularity;
	}

	public int days() {
		return days;
	}

	public DashboardGranularity granularity() {
		return granularity;
	}

}
