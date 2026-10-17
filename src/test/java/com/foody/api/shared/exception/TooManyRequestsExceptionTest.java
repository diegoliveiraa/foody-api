package com.foody.api.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.shared.probe.ProbeTooManyRequestsException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class TooManyRequestsExceptionTest {

	@Test
	void tellsTheClientToRetryAfterTheWaitRoundedUpToWholeSeconds() {
		Duration wait = Duration.ofMillis(1_500);

		HttpHeaders headers = new ProbeTooManyRequestsException(wait).headers();

		assertThat(headers.getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("2");
	}

	@Test
	void neverTellsTheClientToRetryInLessThanOneSecond() {
		Duration wait = Duration.ZERO;

		HttpHeaders headers = new ProbeTooManyRequestsException(wait).headers();

		assertThat(headers.getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
	}

}
