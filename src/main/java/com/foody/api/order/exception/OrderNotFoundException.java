package com.foody.api.order.exception;

import com.foody.api.shared.exception.NotFoundException;
import com.foody.api.shared.message.Messages;
import java.util.UUID;

public class OrderNotFoundException extends NotFoundException {

	public OrderNotFoundException(UUID id) {
		super(Messages.ORDER_NOT_FOUND, id.toString());
	}

}
