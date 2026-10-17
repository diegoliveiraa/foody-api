package com.foody.api.shared.ratelimit;

import com.foody.api.shared.util.EmailUtils;
import com.foody.api.shared.util.HashUtils;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.util.Optional;

public class RateLimiter {

	private final RateLimitProperties properties;

	private final TimeMeter timeMeter;

	private final Cache<String, Bucket> buckets;

	public RateLimiter(RateLimitProperties properties, TimeMeter timeMeter) {
		this.properties = properties;
		this.timeMeter = timeMeter;
		this.buckets = Caffeine.newBuilder()
			.maximumSize(properties.maxTrackedKeys())
			.expireAfterAccess(properties.longestPeriod())
			.build();
	}

	public Optional<Duration> tryConsume(RateLimitTarget target) {
		String key = target.policy().name() + RateLimitConstants.KEY_SEPARATOR + target.identifier();
		Bucket bucket = buckets.get(key, ignored -> newBucket(target.policy().limitIn(properties)));
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(RateLimitConstants.TOKENS_PER_REQUEST);
		if (probe.isConsumed()) {
			return Optional.empty();
		}
		return Optional.of(Duration.ofNanos(probe.getNanosToWaitForRefill()));
	}

	public void consumeLoginAttempt(String email, String address) {
		String identifier = HashUtils.sha256Hex(EmailUtils.normalize(email)) + RateLimitConstants.KEY_SEPARATOR
				+ address;
		Optional<Duration> wait = tryConsume(new RateLimitTarget(RateLimitPolicy.LOGIN_PER_EMAIL, identifier));
		if (wait.isPresent()) {
			throw new RateLimitExceededException(wait.get());
		}
	}

	private Bucket newBucket(RateLimitProperties.Limit limit) {
		return Bucket.builder()
			.addLimit(bandwidth -> bandwidth.capacity(limit.capacity()).refillGreedy(limit.capacity(), limit.period()))
			.withCustomTimePrecision(timeMeter)
			.build();
	}

}
