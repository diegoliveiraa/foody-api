package com.foody.api.user.exception;

import com.foody.api.shared.exception.NotFoundException;
import com.foody.api.shared.message.Messages;
import java.util.UUID;

public class UserNotFoundException extends NotFoundException {

	public UserNotFoundException(UUID id) {
		super(Messages.RESOURCE_NOT_FOUND, id.toString());
	}

}
