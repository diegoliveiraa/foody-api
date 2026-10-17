package com.foody.api.dashboard.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DashboardMathUtilsTest {

	@ParameterizedTest
	@CsvSource({ "1, 3, 33.3", "2, 3, 66.7", "1, 16, 6.3", "-1, 16, -6.3", "-1, 3, -33.3", "0, 5, 0.0", "3, 2, 150.0" })
	void percentOfRoundsHalfUpToOneDecimal(long part, long whole, BigDecimal expected) {
		BigDecimal percent = DashboardMathUtils.percentOf(part, whole);

		assertThat(percent).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "0, 0", "5, 0", "-2, 0" })
	void percentOfNothingIsNull(long part, long whole) {
		BigDecimal percent = DashboardMathUtils.percentOf(part, whole);

		assertThat(percent).isNull();
	}

	@ParameterizedTest
	@CsvSource({ "390, 7", "389.99, 6", "30, 1", "29.9, 0", "1530.5, 26" })
	void roundedMinutesRoundHalfUp(BigDecimal seconds, long expected) {
		Long minutes = DashboardMathUtils.roundedMinutes(seconds);

		assertThat(minutes).isEqualTo(expected);
	}

	@Test
	void roundedMinutesOfNoSampleIsNull() {
		Long minutes = DashboardMathUtils.roundedMinutes(null);

		assertThat(minutes).isNull();
	}

	@ParameterizedTest
	@CsvSource({ "20.01, 2, 10.00", "20.03, 2, 10.02", "30.02, 3, 10.01", "59.90, 1, 59.90" })
	void averageAmountKeepsTheMoneyScaleAndRounding(BigDecimal total, long count, BigDecimal expected) {
		BigDecimal average = DashboardMathUtils.averageAmount(total, count);

		assertThat(average).isEqualTo(expected);
	}

	@Test
	void averageAmountOfNothingIsNull() {
		BigDecimal average = DashboardMathUtils.averageAmount(BigDecimal.ZERO, 0);

		assertThat(average).isNull();
	}

}
