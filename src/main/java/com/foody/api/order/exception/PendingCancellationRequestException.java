package com.foody.api.order.exception;

import com.foody.api.shared.exception.BusinessRuleException;
import com.foody.api.shared.message.Messages;

public class PendingCancellationRequestException extends BusinessRuleException {

	public PendingCancellationRequestException() {
		super(Messages.PENDING_CANCELLATION_REQUEST);
	}

}
