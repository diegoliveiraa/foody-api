package com.foody.api.dashboard.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.shared.restaurant.RestaurantProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DashboardWindowTest {

	private static final RestaurantProperties SAO_PAULO = new RestaurantProperties(ZoneId.of("America/Sao_Paulo"));

	private static final Instant AFTERNOON = Instant.parse("2026-05-20T18:00:00Z");

	@Test
	void todayWindowStartsAtLocalMidnight() {
		Clock clock = Clock.fixed(AFTERNOON, ZoneOffset.UTC);

		DashboardWindow window = DashboardWindow.of(DashboardPeriod.TODAY, SAO_PAULO, clock);

		assertThat(window.period()).isEqualTo(DashboardPeriod.TODAY);
		assertThat(window.zone()).isEqualTo(SAO_PAULO.timeZone());
		assertThat(window.start()).isEqualTo(Instant.parse("2026-05-20T03:00:00Z"));
		assertThat(window.end()).isEqualTo(AFTERNOON);
	}

	@ParameterizedTest
	@CsvSource({ "TODAY, 2026-05-20T03:00:00Z, 2026-05-19T03:00:00Z, 2026-05-19T18:00:00Z",
			"LAST_7_DAYS, 2026-05-14T03:00:00Z, 2026-05-07T03:00:00Z, 2026-05-13T18:00:00Z",
			"LAST_30_DAYS, 2026-04-21T03:00:00Z, 2026-03-22T03:00:00Z, 2026-04-20T18:00:00Z" })
	void previousWindowIsShiftedByThePeriodLength(DashboardPeriod period, Instant start, Instant previousStart,
			Instant previousEnd) {
		Clock clock = Clock.fixed(AFTERNOON, ZoneOffset.UTC);

		DashboardWindow window = DashboardWindow.of(period, SAO_PAULO, clock);

		assertThat(window.start()).isEqualTo(start);
		assertThat(window.end()).isEqualTo(AFTERNOON);
		assertThat(window.previousStart()).isEqualTo(previousStart);
		assertThat(window.previousEnd()).isEqualTo(previousEnd);
	}

	@ParameterizedTest
	@CsvSource({ "TODAY, 2026-03-10T03:00:00Z", "LAST_7_DAYS, 2026-03-04T03:00:00Z",
			"LAST_30_DAYS, 2026-02-09T03:00:00Z" })
	void windowsUseTheRestaurantDateNotTheUtcDate(DashboardPeriod period, Instant start) {
		Clock clock = Clock.fixed(Instant.parse("2026-03-11T01:30:00Z"), ZoneOffset.UTC);

		DashboardWindow window = DashboardWindow.of(period, SAO_PAULO, clock);

		assertThat(window.start()).isEqualTo(start);
	}

	@Test
	void todayBucketsRunHourlyFromMidnightThroughTheCurrentHour() {
		Clock clock = Clock.fixed(Instant.parse("2026-05-20T17:35:00Z"), ZoneOffset.UTC);

		List<Instant> buckets = DashboardWindow.of(DashboardPeriod.TODAY, SAO_PAULO, clock).bucketStarts();

		assertThat(buckets).containsExactlyElementsOf(IntStream.rangeClosed(3, 17)
			.mapToObj(hour -> Instant.parse("2026-05-20T%02d:00:00Z".formatted(hour)))
			.toList());
	}

	@Test
	void dayBucketsStartAtEachLocalMidnight() {
		Clock clock = Clock.fixed(Instant.parse("2026-05-21T01:30:00Z"), ZoneOffset.UTC);

		List<Instant> lastWeek = DashboardWindow.of(DashboardPeriod.LAST_7_DAYS, SAO_PAULO, clock).bucketStarts();
		List<Instant> lastMonth = DashboardWindow.of(DashboardPeriod.LAST_30_DAYS, SAO_PAULO, clock).bucketStarts();

		assertThat(lastWeek).containsExactlyElementsOf(IntStream.rangeClosed(14, 20)
			.mapToObj(day -> Instant.parse("2026-05-%02dT03:00:00Z".formatted(day)))
			.toList());
		assertThat(lastMonth).hasSize(DashboardPeriod.LAST_30_DAYS.days())
			.startsWith(Instant.parse("2026-04-21T03:00:00Z"))
			.endsWith(Instant.parse("2026-05-20T03:00:00Z"));
	}

}
