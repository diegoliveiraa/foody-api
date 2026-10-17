package com.foody.api.shared.ratelimit;

import java.util.function.Function;

public enum RateLimitPolicy {

	LOGIN(RateLimitProperties::login), LOGIN_PER_EMAIL(RateLimitProperties::loginPerEmail),
	REGISTER(RateLimitProperties::register), REFRESH(RateLimitProperties::refresh),
	PLACE_ORDER(RateLimitProperties::placeOrder), REQUEST_CANCELLATION(RateLimitProperties::requestCancellation),
	AUTHENTICATED(RateLimitProperties::authenticated), ANONYMOUS(RateLimitProperties::anonymous);

	private final Function<RateLimitProperties, RateLimitProperties.Limit> limit;

	RateLimitPolicy(Function<RateLimitProperties, RateLimitProperties.Limit> limit) {
		this.limit = limit;
	}

	public RateLimitProperties.Limit limitIn(RateLimitProperties properties) {
		return limit.apply(properties);
	}

}
