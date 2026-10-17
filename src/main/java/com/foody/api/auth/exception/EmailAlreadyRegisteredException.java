package com.foody.api.auth.exception;

import com.foody.api.shared.exception.ConflictException;
import com.foody.api.shared.message.Messages;

public class EmailAlreadyRegisteredException extends ConflictException {

	public EmailAlreadyRegisteredException() {
		super(Messages.EMAIL_ALREADY_REGISTERED);
	}

}
