package com.foody.api.order.exception;

import com.foody.api.shared.exception.BusinessRuleException;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.pagination.PaginationConstants;
import java.util.List;

public class ProductUnavailableException extends BusinessRuleException {

	public ProductUnavailableException(List<String> unavailableProducts) {
		super(Messages.PRODUCT_UNAVAILABLE, String.join(PaginationConstants.LIST_DELIMITER, unavailableProducts));
	}

}
