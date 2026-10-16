package com.foody.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SeedScaleTest {

	@ParameterizedTest
	@CsvSource({ "SMALL, 20", "MEDIUM, 200", "LARGE, 2000" })
	void multipliesTheSeedersBaseCountByTheScale(SeedScale scale, int expectedCount) {
		int baseCount = 20;

		int count = scale.of(baseCount);

		assertThat(count).isEqualTo(expectedCount);
	}

}
