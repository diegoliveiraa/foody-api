package com.foody.api.order.exception;

import com.foody.api.shared.exception.BusinessRuleException;
import com.foody.api.shared.message.Messages;

public class InvalidStatusTransitionException extends BusinessRuleException {

	public InvalidStatusTransitionException() {
		super(Messages.INVALID_STATUS_TRANSITION);
	}

}
