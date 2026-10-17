package com.foody.api.shared.exception;

import com.foody.api.shared.message.Messages;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public abstract class UnauthorizedException extends DomainException {

	protected UnauthorizedException(Messages messageKey) {
		super(messageKey);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.UNAUTHORIZED;
	}

	@Override
	public HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.WWW_AUTHENTICATE, ErrorConstants.BEARER_CHALLENGE);
		return headers;
	}

}
