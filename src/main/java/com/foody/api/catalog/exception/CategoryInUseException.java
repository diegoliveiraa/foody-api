package com.foody.api.catalog.exception;

import com.foody.api.shared.exception.ConflictException;
import com.foody.api.shared.message.Messages;

public class CategoryInUseException extends ConflictException {

	public CategoryInUseException() {
		super(Messages.CATEGORY_IN_USE);
	}

}
