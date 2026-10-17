package com.foody.api.shared.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class RestaurantPropertiesTest {

	private static final RestaurantProperties SAO_PAULO = new RestaurantProperties(ZoneId.of("America/Sao_Paulo"));

	@Test
	void aRestaurantDayStartsAtLocalMidnight() {
		LocalDate day = LocalDate.of(2026, 10, 17);

		Instant start = SAO_PAULO.startOf(day);
		Instant end = SAO_PAULO.startOfDayAfter(day);

		assertThat(start).isEqualTo(Instant.parse("2026-10-17T03:00:00Z"));
		assertThat(end).isEqualTo(Instant.parse("2026-10-18T03:00:00Z"));
	}

	@Test
	void anInstantAfterMidnightInUtcCanStillBelongToThePreviousRestaurantDay() {
		Instant lateEvening = Instant.parse("2026-10-18T02:30:00Z");

		LocalDate day = SAO_PAULO.dayOf(lateEvening);

		assertThat(day).isEqualTo(LocalDate.of(2026, 10, 17));
	}

}
