package com.foody.api.shared.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = RateLimitConstants.PROPERTIES_PREFIX)
public record RateLimitProperties(@Positive long maxTrackedKeys, @Valid @NotNull Limit login,
		@Valid @NotNull Limit loginPerEmail, @Valid @NotNull Limit register, @Valid @NotNull Limit refresh,
		@Valid @NotNull Limit placeOrder, @Valid @NotNull Limit requestCancellation,
		@Valid @NotNull Limit authenticated, @Valid @NotNull Limit anonymous) {

	public Duration longestPeriod() {
		return Arrays.stream(RateLimitPolicy.values())
			.map(policy -> policy.limitIn(this).period())
			.max(Comparator.naturalOrder())
			.orElseThrow();
	}

	public record Limit(@Positive long capacity, @NotNull @DurationMin(seconds = 1) Duration period) {
	}

}
