package com.foody.api.order.validation;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.dto.CancelOrderRequest;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.shared.message.Messages;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;

public class CancellationNoteValidator implements ConstraintValidator<CancellationNote, CancelOrderRequest> {

	@Override
	public boolean isValid(CancelOrderRequest request, ConstraintValidatorContext context) {
		boolean needsNote = Optional.ofNullable(request)
			.map(CancelOrderRequest::reason)
			.filter(CancellationReason::requiresNote)
			.isPresent();
		if (!needsNote || request.note() != null) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(Messages.CANCELLATION_NOTE_REQUIRED.text())
			.addPropertyNode(OrderConstants.NOTE_ATTRIBUTE)
			.addConstraintViolation();
		return false;
	}

}
