package com.foody.api.dashboard.dto;

import com.foody.api.dashboard.DashboardConstants;
import com.foody.api.shared.persistence.PersistenceConstants;
import com.foody.api.shared.restaurant.RestaurantProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

public record DashboardWindow(DashboardPeriod period, ZoneId zone, Instant start, Instant end, Instant previousStart,
		Instant previousEnd) {

	public static DashboardWindow of(DashboardPeriod period, RestaurantProperties restaurant, Clock clock) {
		Instant now = clock.instant().truncatedTo(PersistenceConstants.TIMESTAMP_PRECISION);
		ZoneId zone = restaurant.timeZone();
		Instant start = restaurant.startOfDayAfter(restaurant.dayOf(now).minusDays(period.days()));
		return new DashboardWindow(period, zone, start, now, shiftedBack(start, period, zone),
				shiftedBack(now, period, zone));
	}

	public List<Instant> bucketStarts() {
		ChronoUnit unit = period.granularity().unit();
		return Stream
			.iterate(start.atZone(zone), bucket -> !bucket.toInstant().isAfter(end),
					bucket -> bucket.plus(DashboardConstants.BUCKET_STEP, unit))
			.map(ZonedDateTime::toInstant)
			.toList();
	}

	private static Instant shiftedBack(Instant instant, DashboardPeriod period, ZoneId zone) {
		return instant.atZone(zone).minusDays(period.days()).toInstant();
	}

}
