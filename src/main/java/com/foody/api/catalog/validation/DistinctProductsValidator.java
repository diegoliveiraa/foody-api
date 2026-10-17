package com.foody.api.catalog.validation;

import com.foody.api.catalog.dto.CartItem;
import com.foody.api.shared.message.Messages;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class DistinctProductsValidator implements ConstraintValidator<DistinctProducts, List<CartItem>> {

	@Override
	public boolean isValid(List<CartItem> items, ConstraintValidatorContext context) {
		if (items == null) {
			return true;
		}
		List<UUID> productIds = items.stream()
			.filter(Objects::nonNull)
			.map(CartItem::productId)
			.filter(Objects::nonNull)
			.toList();
		Set<UUID> distinct = new HashSet<>(productIds);
		if (distinct.size() == productIds.size()) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(Messages.DUPLICATE_CART_PRODUCT.text()).addConstraintViolation();
		return false;
	}

}
