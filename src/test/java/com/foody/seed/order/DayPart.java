package com.foody.seed.order;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.datafaker.Faker;

public enum DayPart {

	LUNCH(LocalTime.of(11, 0), LocalTime.of(14, 0), 45), DINNER(LocalTime.of(18, 0), LocalTime.of(22, 0), 45),
	OFF_PEAK(LocalTime.of(10, 0), LocalTime.of(23, 0), 10);

	private static final List<DayPart> BY_WEIGHT = Arrays.stream(values())
		.flatMap(part -> Collections.nCopies(part.weight, part).stream())
		.toList();

	private final LocalTime start;

	private final LocalTime end;

	private final int weight;

	DayPart(LocalTime start, LocalTime end, int weight) {
		this.start = start;
		this.end = end;
		this.weight = weight;
	}

	public static LocalTime drawTime(Faker faker) {
		DayPart part = faker.options().nextElement(BY_WEIGHT);
		return LocalTime
			.ofSecondOfDay(faker.number().numberBetween(part.start.toSecondOfDay(), part.end.toSecondOfDay()));
	}

}
