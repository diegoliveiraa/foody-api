package com.foody.api.catalog.exception;

import com.foody.api.shared.exception.ConflictException;
import com.foody.api.shared.message.Messages;

public class CategoryNameTakenException extends ConflictException {

	public CategoryNameTakenException() {
		super(Messages.CATEGORY_NAME_TAKEN);
	}

}
