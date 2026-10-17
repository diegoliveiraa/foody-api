package com.foody.api.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class LikeUtilsTest {

	@Test
	void containingWrapsTheTermInWildcards() {
		String term = "ana";

		String pattern = LikeUtils.containing(term);

		assertThat(pattern).isEqualTo("%ana%");
	}

	@Test
	void containingEscapesTheWildcardsAndTheEscapeCharacterOfTheTerm() {
		String term = "50%_off\\now";

		String pattern = LikeUtils.containing(term);

		assertThat(pattern).isEqualTo("%50\\%\\_off\\\\now%");
	}

	@Test
	void searchPatternIgnoresCaseAndSurroundingSpaces() {
		String search = "  Ana 50% ";

		Optional<String> pattern = LikeUtils.searchPattern(search);

		assertThat(pattern).contains("%ana 50\\%%");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   " })
	void searchPatternIsEmptyForABlankSearch(String search) {
		Optional<String> pattern = LikeUtils.searchPattern(search);

		assertThat(pattern).isEmpty();
	}

}
