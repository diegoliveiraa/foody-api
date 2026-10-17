package com.foody.api.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.auth.AuthConstants;
import com.foody.api.auth.repository.RefreshTokenRepository;
import com.foody.api.shared.security.SecurityConstants;
import com.foody.api.shared.util.HashUtils;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AuthClient;
import com.foody.testsupport.security.TestCredentials;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class LogoutTest {

	private static final String COOKIE = AuthConstants.REFRESH_COOKIE_NAME;

	@Autowired
	private AuthClient authClient;

	@Autowired
	private UserFactory users;

	@Autowired
	private RefreshTokenRepository refreshTokens;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void logoutRevokesFamilyAndClearsCookie() {
		String email = users.create(user -> user.changePassword(passwordEncoder.encode(TestCredentials.PASSWORD)))
			.getEmail();
		String loginToken = authClient.refreshTokenOf(authClient.login(email, TestCredentials.PASSWORD));
		String rotatedToken = authClient.refreshTokenOf(authClient.refresh(loginToken));

		MvcTestResult result = authClient.logout(rotatedToken);

		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(result).cookies()
			.hasValue(COOKIE, AuthConstants.CLEARED_COOKIE_VALUE)
			.hasMaxAge(COOKIE, Duration.ZERO)
			.hasPath(COOKIE, SecurityConstants.AUTH_PATH)
			.isHttpOnly(COOKIE, true)
			.isSecure(COOKIE, true);
		assertThat(refreshTokens.findByTokenHash(HashUtils.sha256Hex(rotatedToken)).orElseThrow().isRevoked()).isTrue();
		assertThat(refreshTokens.findByTokenHash(HashUtils.sha256Hex(loginToken)).orElseThrow().isRevoked()).isTrue();
	}

	@Test
	void logoutWithoutCookieStillAnswersNoContent() {
		String noToken = null;

		MvcTestResult result = authClient.logout(noToken);

		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(result).cookies().hasMaxAge(COOKIE, Duration.ZERO);
	}

	@Test
	void logoutWithAnUnknownCookieStillAnswersNoContent() {
		String unknownToken = "not-a-token-the-api-issued";

		MvcTestResult result = authClient.logout(unknownToken);

		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(result).cookies().hasMaxAge(COOKIE, Duration.ZERO);
	}

}
