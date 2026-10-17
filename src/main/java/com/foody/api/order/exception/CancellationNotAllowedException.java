package com.foody.api.order.exception;

import com.foody.api.shared.exception.BusinessRuleException;
import com.foody.api.shared.message.Messages;

public class CancellationNotAllowedException extends BusinessRuleException {

	public CancellationNotAllowedException() {
		super(Messages.CANCELLATION_NOT_ALLOWED);
	}

}
