package com.foody.api.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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

}
