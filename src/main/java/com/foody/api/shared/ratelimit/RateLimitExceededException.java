package com.foody.api.shared.ratelimit;

import com.foody.api.shared.exception.TooManyRequestsException;
import com.foody.api.shared.message.Messages;
import java.time.Duration;

public class RateLimitExceededException extends TooManyRequestsException {

	public RateLimitExceededException(Duration retryAfter) {
		super(Messages.RATE_LIMITED, retryAfter);
	}

}
