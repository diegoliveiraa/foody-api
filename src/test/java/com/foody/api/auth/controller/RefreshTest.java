package com.foody.api.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.auth.AuthConstants;
import com.foody.api.auth.repository.RefreshTokenRepository;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.util.HashUtils;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AuthClient;
import com.foody.testsupport.security.TestCredentials;
import com.foody.testsupport.time.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class RefreshTest {

	private static final String COOKIE = AuthConstants.REFRESH_COOKIE_NAME;

	private static final Duration ROTATION_INTERVAL = Duration.ofDays(6);

	@Autowired
	private AuthClient authClient;

	@Autowired
	private UserFactory users;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokens;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MutableClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void refreshRotatesTheRefreshToken() {
		String loginToken = authClient.refreshTokenOf(authClient.login(customerEmail(), TestCredentials.PASSWORD));

		MvcTestResult result = authClient.refresh(loginToken);

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
		assertThat(result).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
		assertThat(result).cookies().hasMaxAge(COOKIE, Duration.ofDays(7));
		String rotatedToken = authClient.refreshTokenOf(result);
		assertThat(rotatedToken).isNotEqualTo(loginToken);
		assertThat(refreshTokens.findByTokenHash(HashUtils.sha256Hex(loginToken)).orElseThrow().isRevoked()).isTrue();
		assertThat(refreshTokens.findByTokenHash(HashUtils.sha256Hex(rotatedToken)).orElseThrow().isRevoked())
			.isFalse();
	}

	@Test
	void refreshWithRotatedTokenRevokesTheWholeFamily() {
		String loginToken = authClient.refreshTokenOf(authClient.login(customerEmail(), TestCredentials.PASSWORD));
		String rotatedToken = authClient.refreshTokenOf(authClient.refresh(loginToken));

		MvcTestResult result = authClient.refresh(loginToken);

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.REFRESH_TOKEN_INVALID.name());
		assertThat(refreshTokens.findByTokenHash(HashUtils.sha256Hex(rotatedToken)).orElseThrow().isRevoked()).isTrue();
	}

	@Test
	void refreshNeverRenewsPastTheEndOfTheFamily() {
		clock.fixAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		String token = authClient.refreshTokenOf(authClient.login(customerEmail(), TestCredentials.PASSWORD));
		for (int rotation = 0; rotation < 4; rotation++) {
			clock.advance(ROTATION_INTERVAL);
			token = authClient.refreshTokenOf(authClient.refresh(token));
		}
		clock.advance(Duration.ofDays(5));

		MvcTestResult result = authClient.refresh(token);

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).cookies().hasMaxAge(COOKIE, Duration.ofDays(1));
	}

	@Test
	void refreshRejectsFamilyOlderThanThirtyDays() {
		clock.fixAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		String token = authClient.refreshTokenOf(authClient.login(customerEmail(), TestCredentials.PASSWORD));
		for (int rotation = 0; rotation < 4; rotation++) {
			clock.advance(ROTATION_INTERVAL);
			token = authClient.refreshTokenOf(authClient.refresh(token));
		}
		clock.advance(Duration.ofDays(5));
		token = authClient.refreshTokenOf(authClient.refresh(token));
		clock.advance(Duration.ofDays(1).plusMinutes(1));

		MvcTestResult result = authClient.refresh(token);

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.REFRESH_TOKEN_INVALID.name());
	}

	@Test
	void refreshRejectsATokenPastItsSevenDays() {
		clock.fixAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		String token = authClient.refreshTokenOf(authClient.login(customerEmail(), TestCredentials.PASSWORD));
		clock.advance(Duration.ofDays(7).plusMinutes(1));

		MvcTestResult result = authClient.refresh(token);

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.REFRESH_TOKEN_INVALID.name());
	}

	@Test
	void refreshRejectsTheTokenOfADisabledUser() {
		User customer = users.create(user -> user.changePassword(passwordEncoder.encode(TestCredentials.PASSWORD)));
		String token = authClient.refreshTokenOf(authClient.login(customer.getEmail(), TestCredentials.PASSWORD));
		customer.disable();
		userRepository.save(customer);

		MvcTestResult result = authClient.refresh(token);

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.REFRESH_TOKEN_INVALID.name());
	}

	@Test
	void refreshWithUnknownTokenAnswersRefreshTokenInvalid() {
		String unknownToken = "not-a-token-the-api-issued";

		MvcTestResult result = authClient.refresh(unknownToken);

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.REFRESH_TOKEN_INVALID.name());
	}

	@Test
	void refreshWithoutCookieAnswersRefreshTokenInvalid() {
		String noToken = null;

		MvcTestResult result = authClient.refresh(noToken);

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.REFRESH_TOKEN_INVALID.name());
	}

	private String customerEmail() {
		return users.create(user -> user.changePassword(passwordEncoder.encode(TestCredentials.PASSWORD))).getEmail();
	}

}
