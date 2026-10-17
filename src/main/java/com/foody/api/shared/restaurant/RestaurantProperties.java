package com.foody.api.shared.restaurant;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = RestaurantConstants.PROPERTIES_PREFIX)
public record RestaurantProperties(@NotNull ZoneId timeZone) {

	public Instant startOf(LocalDate day) {
		return day.atStartOfDay(timeZone).toInstant();
	}

	public Instant startOfDayAfter(LocalDate day) {
		return startOf(day.plusDays(1));
	}

	public LocalDate dayOf(Instant instant) {
		return LocalDate.ofInstant(instant, timeZone);
	}

}
