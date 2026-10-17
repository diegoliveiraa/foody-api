package com.foody.api.catalog.exception;

import com.foody.api.shared.exception.ConflictException;
import com.foody.api.shared.message.Messages;

public class ProductInUseException extends ConflictException {

	public ProductInUseException() {
		super(Messages.PRODUCT_IN_USE);
	}

}
