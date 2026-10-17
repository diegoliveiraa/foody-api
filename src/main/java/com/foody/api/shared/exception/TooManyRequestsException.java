package com.foody.api.shared.exception;

import com.foody.api.shared.message.Messages;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public abstract class TooManyRequestsException extends DomainException {

	private final Duration retryAfter;

	protected TooManyRequestsException(Messages messageKey, Duration retryAfter) {
		super(messageKey);
		this.retryAfter = retryAfter;
	}

	public long retryAfterSeconds() {
		long seconds = Math.ceilDiv(retryAfter.toMillis(), ErrorConstants.MILLIS_PER_SECOND);
		return Math.max(ErrorConstants.MIN_RETRY_AFTER_SECONDS, seconds);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.TOO_MANY_REQUESTS;
	}

	@Override
	public HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds()));
		return headers;
	}

}
