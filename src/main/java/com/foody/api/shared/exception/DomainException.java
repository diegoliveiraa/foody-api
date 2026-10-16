package com.foody.api.shared.exception;

import com.foody.api.shared.message.Messages;
import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {

	private final Messages messageKey;

	protected DomainException(Messages messageKey) {
		super(messageKey.text());
		this.messageKey = messageKey;
	}

	protected DomainException(Messages messageKey, String detail) {
		super(messageKey.text() + ErrorConstants.DETAIL_SEPARATOR + detail);
		this.messageKey = messageKey;
	}

	protected DomainException(Messages messageKey, Throwable cause) {
		super(messageKey.text(), cause);
		this.messageKey = messageKey;
	}

	public Messages messageKey() {
		return messageKey;
	}

	public abstract HttpStatus status();

}
