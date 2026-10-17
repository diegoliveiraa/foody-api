package com.foody.api.catalog.exception;

import com.foody.api.shared.exception.BusinessRuleException;
import com.foody.api.shared.message.Messages;
import java.util.UUID;

public class UnknownCategoryException extends BusinessRuleException {

	public UnknownCategoryException(UUID id) {
		super(Messages.UNKNOWN_CATEGORY, id.toString());
	}

}
