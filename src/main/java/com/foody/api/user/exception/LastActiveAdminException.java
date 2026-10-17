package com.foody.api.user.exception;

import com.foody.api.shared.exception.BusinessRuleException;
import com.foody.api.shared.message.Messages;

public class LastActiveAdminException extends BusinessRuleException {

	public LastActiveAdminException() {
		super(Messages.LAST_ACTIVE_ADMIN);
	}

}
