package com.foody.api.auth.exception;

import com.foody.api.shared.exception.UnauthorizedException;
import com.foody.api.shared.message.Messages;

public class RefreshTokenInvalidException extends UnauthorizedException {

	public RefreshTokenInvalidException() {
		super(Messages.REFRESH_TOKEN_INVALID);
	}

}
