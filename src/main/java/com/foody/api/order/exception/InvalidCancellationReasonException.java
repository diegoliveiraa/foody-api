package com.foody.api.order.exception;

import com.foody.api.shared.exception.BusinessRuleException;
import com.foody.api.shared.message.Messages;

public class InvalidCancellationReasonException extends BusinessRuleException {

	public InvalidCancellationReasonException() {
		super(Messages.INVALID_CANCELLATION_REASON);
	}

}
