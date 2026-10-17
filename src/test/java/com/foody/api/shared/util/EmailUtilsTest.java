package com.foody.api.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailUtilsTest {

	@Test
	void trimsAndLowercasesAnEmail() {
		String email = "  Ana.Souza@Foody.DEV ";

		String normalized = EmailUtils.normalize(email);

		assertThat(normalized).isEqualTo("ana.souza@foody.dev");
	}

	@Test
	void keepsAMissingEmailMissing() {
		String email = null;

		String normalized = EmailUtils.normalize(email);

		assertThat(normalized).isNull();
	}

}
