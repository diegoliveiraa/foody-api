package com.foody.api.auth.validation;

import com.foody.api.auth.AuthConstants;
import com.foody.api.shared.exception.ErrorConstants;
import com.foody.api.shared.message.Messages;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class PasswordValidator implements ConstraintValidator<Password, String> {

	@Override
	public boolean isValid(String password, ConstraintValidatorContext context) {
		if (password == null) {
			return true;
		}
		Optional<String> violation = violationOf(password);
		violation.ifPresent(message -> report(message, context));
		return violation.isEmpty();
	}

	private Optional<String> violationOf(String password) {
		if (password.codePointCount(0, password.length()) < AuthConstants.PASSWORD_MIN_LENGTH) {
			return Optional.of(Messages.PASSWORD_TOO_SHORT.text() + ErrorConstants.DETAIL_SEPARATOR
					+ AuthConstants.PASSWORD_MIN_LENGTH);
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > AuthConstants.PASSWORD_MAX_BYTES) {
			return Optional.of(Messages.PASSWORD_TOO_LONG.text() + ErrorConstants.DETAIL_SEPARATOR
					+ AuthConstants.PASSWORD_MAX_BYTES);
		}
		return Optional.empty();
	}

	private void report(String message, ConstraintValidatorContext context) {
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
	}

}
