package com.foody.api.shared.ratelimit;

import io.github.bucket4j.TimeMeter;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ClockTimeMeter implements TimeMeter {

	private final Clock clock;

	public ClockTimeMeter(Clock clock) {
		this.clock = clock;
	}

	@Override
	public long currentTimeNanos() {
		return ChronoUnit.NANOS.between(Instant.EPOCH, clock.instant());
	}

	@Override
	public boolean isWallClockBased() {
		return true;
	}

}
