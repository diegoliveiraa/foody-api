package com.foody.api.shared.ratelimit;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record RateLimitCaller(@Nullable String userName, String address) {

	public Optional<String> user() {
		return Optional.ofNullable(userName);
	}

}
