package com.foody.testsupport.time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MutableClock extends Clock {

	private final AtomicReference<Instant> fixedInstant = new AtomicReference<>();

	@Override
	public Instant instant() {
		return Optional.ofNullable(fixedInstant.get()).orElseGet(Instant::now);
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return Clock.fixed(instant(), zone);
	}

	public void fixAt(Instant instant) {
		fixedInstant.set(instant);
	}

	public void advance(Duration duration) {
		fixedInstant.set(instant().plus(duration));
	}

	public void reset() {
		fixedInstant.set(null);
	}

}
