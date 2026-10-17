package com.foody.api.order.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ZipCodeUtilsTest {

	@ParameterizedTest
	@ValueSource(strings = { "01310-100", "01310100" })
	void zipCodeWithOrWithoutHyphenIsValid(String zipCode) {
		boolean valid = ZipCodeUtils.isValid(zipCode);

		assertThat(valid).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "1310-100", "01310-1000", "0131-0100", "abcde-fgh", "01310 100", "" })
	void zipCodeOutsideTheFormatIsInvalid(String zipCode) {
		boolean valid = ZipCodeUtils.isValid(zipCode);

		assertThat(valid).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "01310-100", "01310100" })
	void normalizedZipCodeAlwaysCarriesTheHyphen(String zipCode) {
		String normalized = ZipCodeUtils.normalize(zipCode);

		assertThat(normalized).isEqualTo("01310-100");
	}

}
