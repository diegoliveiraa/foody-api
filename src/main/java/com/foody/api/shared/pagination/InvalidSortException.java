package com.foody.api.shared.pagination;

import com.foody.api.shared.exception.InvalidRequestException;
import com.foody.api.shared.message.Messages;

public class InvalidSortException extends InvalidRequestException {

	public InvalidSortException(String rejectedProperties) {
		super(Messages.INVALID_SORT, rejectedProperties);
	}

}
