package com.foody.api.shared.probe;

import com.foody.api.shared.exception.UnauthorizedException;
import com.foody.api.shared.message.Messages;

public class ProbeUnauthorizedException extends UnauthorizedException {

	public ProbeUnauthorizedException() {
		super(Messages.INVALID_CREDENTIALS);
	}

}
