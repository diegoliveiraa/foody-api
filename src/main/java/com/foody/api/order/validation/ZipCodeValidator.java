package com.foody.api.order.validation;

import com.foody.api.order.util.ZipCodeUtils;
import com.foody.api.shared.message.Messages;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class ZipCodeValidator implements ConstraintValidator<ZipCode, String> {

	@Override
	public boolean isValid(String zipCode, ConstraintValidatorContext context) {
		if (!StringUtils.hasText(zipCode) || ZipCodeUtils.isValid(zipCode)) {
			return true;
		}
		context.disableDefaultConstraintViolation();
		context.buildConstraintViolationWithTemplate(Messages.INVALID_ZIP_CODE.text()).addConstraintViolation();
		return false;
	}

}
