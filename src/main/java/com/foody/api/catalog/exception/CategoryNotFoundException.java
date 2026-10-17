package com.foody.api.catalog.exception;

import com.foody.api.shared.exception.NotFoundException;
import com.foody.api.shared.message.Messages;
import java.util.UUID;

public class CategoryNotFoundException extends NotFoundException {

	public CategoryNotFoundException(UUID id) {
		super(Messages.CATEGORY_NOT_FOUND, id.toString());
	}

}
