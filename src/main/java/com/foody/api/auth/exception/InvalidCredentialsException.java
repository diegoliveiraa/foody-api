package com.foody.api.auth.exception;

import com.foody.api.shared.exception.UnauthorizedException;
import com.foody.api.shared.message.Messages;

public class InvalidCredentialsException extends UnauthorizedException {

	public InvalidCredentialsException() {
		super(Messages.INVALID_CREDENTIALS);
	}

}
