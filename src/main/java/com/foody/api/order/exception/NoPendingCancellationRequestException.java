package com.foody.api.order.exception;

import com.foody.api.shared.exception.BusinessRuleException;
import com.foody.api.shared.message.Messages;

public class NoPendingCancellationRequestException extends BusinessRuleException {

	public NoPendingCancellationRequestException() {
		super(Messages.NO_PENDING_CANCELLATION_REQUEST);
	}

}
