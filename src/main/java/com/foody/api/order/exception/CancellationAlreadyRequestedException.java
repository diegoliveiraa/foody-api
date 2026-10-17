package com.foody.api.order.exception;

import com.foody.api.shared.exception.BusinessRuleException;
import com.foody.api.shared.message.Messages;

public class CancellationAlreadyRequestedException extends BusinessRuleException {

	public CancellationAlreadyRequestedException() {
		super(Messages.CANCELLATION_ALREADY_REQUESTED);
	}

}
