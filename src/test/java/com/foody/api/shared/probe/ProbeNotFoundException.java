package com.foody.api.shared.probe;

import com.foody.api.shared.exception.NotFoundException;
import com.foody.api.shared.message.Messages;
import java.util.UUID;

public class ProbeNotFoundException extends NotFoundException {

	public ProbeNotFoundException(UUID id) {
		super(Messages.RESOURCE_NOT_FOUND, id.toString());
	}

}
