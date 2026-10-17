package com.foody.api.user.exception;

import com.foody.api.shared.exception.BusinessRuleException;
import com.foody.api.shared.message.Messages;

public class CannotChangeOwnAccountException extends BusinessRuleException {

	public CannotChangeOwnAccountException() {
		super(Messages.CANNOT_CHANGE_OWN_ACCOUNT);
	}

}
