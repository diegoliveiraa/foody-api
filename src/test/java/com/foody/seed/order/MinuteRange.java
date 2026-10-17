package com.foody.seed.order;

import java.time.Duration;
import net.datafaker.Faker;

public record MinuteRange(int min, int max) {

	public Duration draw(Faker faker) {
		return Duration.ofMinutes(faker.number().numberBetween(min, max + 1))
			.plusSeconds(faker.number().numberBetween(0, OrderSeedConstants.SECONDS_PER_MINUTE));
	}

}
