package com.foody.api.shared.ratelimit;

import java.util.Optional;
import java.util.function.Function;

public enum RateLimitKey {

	IP(caller -> Optional.of(caller.address())), USER(RateLimitCaller::user);

	private final Function<RateLimitCaller, Optional<String>> identifier;

	RateLimitKey(Function<RateLimitCaller, Optional<String>> identifier) {
		this.identifier = identifier;
	}

	public Optional<String> identify(RateLimitCaller caller) {
		return identifier.apply(caller);
	}

}
