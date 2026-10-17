package com.foody.api.catalog.exception;

import com.foody.api.shared.exception.NotFoundException;
import com.foody.api.shared.message.Messages;
import java.util.UUID;

public class ProductNotFoundException extends NotFoundException {

	public ProductNotFoundException(UUID id) {
		super(Messages.PRODUCT_NOT_FOUND, id.toString());
	}

}
