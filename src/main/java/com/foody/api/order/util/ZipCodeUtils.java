package com.foody.api.order.util;

import com.foody.api.order.OrderConstants;
import org.springframework.util.StringUtils;

public final class ZipCodeUtils {

	private ZipCodeUtils() {
	}

	public static boolean isValid(String zipCode) {
		return OrderConstants.ZIP_CODE_PATTERN.matcher(zipCode).matches();
	}

	public static String normalize(String zipCode) {
		String digits = StringUtils.delete(zipCode, OrderConstants.ZIP_CODE_SEPARATOR);
		return digits.substring(0, OrderConstants.ZIP_CODE_PREFIX_LENGTH) + OrderConstants.ZIP_CODE_SEPARATOR
				+ digits.substring(OrderConstants.ZIP_CODE_PREFIX_LENGTH);
	}

}
