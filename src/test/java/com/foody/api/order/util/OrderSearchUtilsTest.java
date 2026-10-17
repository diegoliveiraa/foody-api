package com.foody.api.order.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class OrderSearchUtilsTest {

	@ParameterizedTest
	@ValueSource(strings = { "1042", "#1042", "  #1042 " })
	void digitsWithOrWithoutHashAreAnOrderNumber(String search) {
		Optional<Long> number = OrderSearchUtils.orderNumber(search);

		assertThat(number).contains(1042L);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "Ana", "#", "10a42", "# 1042", "1234567890123456789" })
	void anythingElseIsNotAnOrderNumber(String search) {
		Optional<Long> number = OrderSearchUtils.orderNumber(search);

		assertThat(number).isEmpty();
	}

}
