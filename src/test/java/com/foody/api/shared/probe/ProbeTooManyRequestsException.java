package com.foody.api.shared.probe;

import com.foody.api.shared.exception.TooManyRequestsException;
import com.foody.api.shared.message.Messages;
import java.time.Duration;

public class ProbeTooManyRequestsException extends TooManyRequestsException {

	public ProbeTooManyRequestsException(Duration retryAfter) {
		super(Messages.RATE_LIMITED, retryAfter);
	}

}
