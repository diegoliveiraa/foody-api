package com.foody.api.shared.ratelimit;

public record RateLimitTarget(RateLimitPolicy policy, String identifier) {

	public static RateLimitTarget generalFor(RateLimitCaller caller) {
		return caller.user()
			.map(user -> new RateLimitTarget(RateLimitPolicy.AUTHENTICATED, user))
			.orElseGet(() -> new RateLimitTarget(RateLimitPolicy.ANONYMOUS, caller.address()));
	}

}
