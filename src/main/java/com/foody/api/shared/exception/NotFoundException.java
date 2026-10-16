package com.foody.api.shared.exception;

import com.foody.api.shared.message.Messages;
import org.springframework.http.HttpStatus;

public abstract class NotFoundException extends DomainException {

	protected NotFoundException(Messages messageKey, String detail) {
		super(messageKey, detail);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}

}
