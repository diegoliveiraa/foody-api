package com.foody.api.shared.exception;

import com.foody.api.shared.message.Messages;
import org.springframework.http.HttpStatus;

public abstract class ConflictException extends DomainException {

	protected ConflictException(Messages messageKey) {
		super(messageKey);
	}

	protected ConflictException(Messages messageKey, String detail) {
		super(messageKey, detail);
	}

	protected ConflictException(Messages messageKey, Throwable cause) {
		super(messageKey, cause);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONFLICT;
	}

}
