package com.foody.api.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HashUtilsTest {

	@Test
	void hashesAValueAsLowercaseHexSha256() {
		String value = "abc";

		String hash = HashUtils.sha256Hex(value);

		assertThat(hash).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
	}

	@Test
	void hashesDifferentValuesDifferently() {
		String first = "token-one";

		String hash = HashUtils.sha256Hex(first);

		assertThat(hash).hasSize(64).isNotEqualTo(HashUtils.sha256Hex("token-two"));
	}

}
