package com.foody.api.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.auth.AuthConstants;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.user.entity.User;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AuthClient;
import com.foody.testsupport.security.TestCredentials;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class LoginTest {

	private static final String COOKIE = AuthConstants.REFRESH_COOKIE_NAME;

	@Autowired
	private AuthClient authClient;

	@Autowired
	private UserFactory users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void loginReturnsAccessTokenAndRefreshCookie() {
		User customer = users.create(user -> user.changePassword(passwordEncoder.encode(TestCredentials.PASSWORD)));

		MvcTestResult result = authClient.login(customer.getEmail(), TestCredentials.PASSWORD);

		assertThat(result).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_JSON);
		assertThat(result).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
		assertThat(result).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
		assertThat(result).cookies()
			.containsCookie(COOKIE)
			.isHttpOnly(COOKIE, true)
			.isSecure(COOKIE, true)
			.hasPath(COOKIE, RouteConstants.AUTH_PATH)
			.hasMaxAge(COOKIE, Duration.ofDays(7));
	}

	@Test
	void loginAcceptsTheEmailInAnyCaseAndWithSurroundingSpaces() {
		User customer = users.create(user -> user.changePassword(passwordEncoder.encode(TestCredentials.PASSWORD)));

		MvcTestResult result = authClient.login("  " + customer.getEmail().toUpperCase() + " ",
				TestCredentials.PASSWORD);

		assertThat(result).hasStatus(HttpStatus.OK);
	}

	@Test
	void loginRejectsWrongPasswordWithInvalidCredentials() {
		User customer = users.create(user -> user.changePassword(passwordEncoder.encode(TestCredentials.PASSWORD)));

		MvcTestResult result = authClient.login(customer.getEmail(), "outra-senha-456");

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_CREDENTIALS.name());
		assertThat(result).cookies().doesNotContainCookie(COOKIE);
	}

	@Test
	void loginRejectsUnknownEmailWithInvalidCredentials() {
		String unknownEmail = "nobody.login@foody.dev";

		MvcTestResult result = authClient.login(unknownEmail, TestCredentials.PASSWORD);

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_CREDENTIALS.name());
	}

	@Test
	void loginRejectsDisabledUserWithInvalidCredentials() {
		User disabled = users.create(user -> {
			user.changePassword(passwordEncoder.encode(TestCredentials.PASSWORD));
			user.disable();
		});

		MvcTestResult result = authClient.login(disabled.getEmail(), TestCredentials.PASSWORD);

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_CREDENTIALS.name());
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo(Messages.INVALID_CREDENTIALS.text());
	}

	@Test
	void loginRejectsPasswordLongerThanSeventyTwoBytesBeforeCheckingIt() {
		User customer = users.create(user -> user.changePassword(passwordEncoder.encode(TestCredentials.PASSWORD)));

		MvcTestResult result = authClient.login(customer.getEmail(), "a".repeat(73));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("password");
	}

}
