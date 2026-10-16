package com.foody.api.shared.exception;

import com.foody.api.shared.message.Messages;
import org.springframework.http.HttpStatus;

public abstract class BusinessRuleException extends DomainException {

	protected BusinessRuleException(Messages messageKey) {
		super(messageKey);
	}

	protected BusinessRuleException(Messages messageKey, String detail) {
		super(messageKey, detail);
	}

	protected BusinessRuleException(Messages messageKey, Throwable cause) {
		super(messageKey, cause);
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.UNPROCESSABLE_CONTENT;
	}

}
