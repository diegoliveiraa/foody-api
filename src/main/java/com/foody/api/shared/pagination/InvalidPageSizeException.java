package com.foody.api.shared.pagination;

import com.foody.api.shared.exception.InvalidRequestException;
import com.foody.api.shared.message.Messages;

public class InvalidPageSizeException extends InvalidRequestException {

	public InvalidPageSizeException() {
		super(Messages.INVALID_PAGE_SIZE, PaginationConstants.ALLOWED_PAGE_SIZES_TEXT);
	}

}
