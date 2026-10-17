package com.foody.api.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.shared.exception.ErrorConstants;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.observability.ObservabilityConstants;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import com.foody.testsupport.security.GrantProbeController;
import com.foody.testsupport.time.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class SecurityChainTest {

	private static final String ALLOWED_ORIGIN = "http://localhost:5173";

	private static final String UNKNOWN_ORIGIN = "https://attacker.example";

	private static final String HEALTH_PATH = "/actuator/health";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserFactory users;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccessTokens tokens;

	@Autowired
	private MutableClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void protectedEndpointWithoutTokenAnswersUnauthenticatedProblemDetail() {
		String requestId = "no-token-1";

		MvcTestResult result = mvc.get()
			.uri(GrantProbeController.SCREEN_PATH)
			.header(ObservabilityConstants.REQUEST_ID_HEADER, requestId)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.hasHeader(HttpHeaders.WWW_AUTHENTICATE, ErrorConstants.BEARER_CHALLENGE);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
		assertThat(result).bodyJson().extractingPath("$.requestId").isEqualTo(requestId);
	}

	@Test
	void protectedEndpointWithAValidTokenOfAGrantedUserAnswers() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));

		MvcTestResult result = mvc.get()
			.uri(GrantProbeController.SCREEN_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(admin))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void tamperedTokenIsRejected() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String tampered = tokens.bearer(admin) + "x";

		MvcTestResult result = mvc.get()
			.uri(GrantProbeController.SCREEN_PATH)
			.header(HttpHeaders.AUTHORIZATION, tampered)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
	}

	@Test
	void tokenOfDisabledUserIsRejected() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String bearer = tokens.bearer(admin);
		admin.disable();
		userRepository.save(admin);

		MvcTestResult result = mvc.get()
			.uri(GrantProbeController.SCREEN_PATH)
			.header(HttpHeaders.AUTHORIZATION, bearer)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED)
			.hasHeader(HttpHeaders.WWW_AUTHENTICATE, ErrorConstants.BEARER_CHALLENGE);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
	}

	@Test
	void tokenOfDeletedUserIsRejected() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String bearer = tokens.bearer(admin);
		userRepository.delete(admin);

		MvcTestResult result = mvc.get()
			.uri(GrantProbeController.SCREEN_PATH)
			.header(HttpHeaders.AUTHORIZATION, bearer)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
	}

	@Test
	void accessTokenIsAcceptedUntilItExpires() {
		clock.fixAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String bearer = tokens.bearer(admin);
		clock.advance(Duration.ofMinutes(14));

		MvcTestResult result = mvc.get()
			.uri(GrantProbeController.SCREEN_PATH)
			.header(HttpHeaders.AUTHORIZATION, bearer)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void expiredAccessTokenIsRejected() {
		clock.fixAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String bearer = tokens.bearer(admin);
		clock.advance(Duration.ofMinutes(17));

		MvcTestResult result = mvc.get()
			.uri(GrantProbeController.SCREEN_PATH)
			.header(HttpHeaders.AUTHORIZATION, bearer)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
	}

	@Test
	void endpointWithoutGrantAnswersForbiddenProblemDetail() {
		String requestId = "customer-on-admin-5";
		User customer = users.create();

		MvcTestResult result = mvc.get()
			.uri(GrantProbeController.GRANT_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(customer))
			.header(ObservabilityConstants.REQUEST_ID_HEADER, requestId)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
		assertThat(result).bodyJson().extractingPath("$.requestId").isEqualTo(requestId);
	}

	@Test
	void endpointWithGrantAnswers() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));

		MvcTestResult result = mvc.get()
			.uri(GrantProbeController.GRANT_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(admin))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void endpointRequiringAScreenAnswersForbiddenToARoleWithoutIt() {
		User staff = users.create(user -> user.changeRole(Role.STAFF));

		MvcTestResult result = mvc.get()
			.uri(GrantProbeController.SCREEN_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(staff))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
	}

	@Test
	void healthIsReachableWithoutToken() {
		String path = HEALTH_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
	}

	@Test
	void corsAllowsConfiguredOriginWithCredentials() {
		String origin = ALLOWED_ORIGIN;

		MvcTestResult result = mvc.options()
			.uri(GrantProbeController.SCREEN_PATH)
			.header(HttpHeaders.ORIGIN, origin)
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK)
			.hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
			.hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, Boolean.TRUE.toString());
	}

	@Test
	void corsExposesRetryAfterAndRequestIdToTheConfiguredOrigin() {
		String origin = ALLOWED_ORIGIN;

		MvcTestResult result = mvc.get().uri(HEALTH_PATH).header(HttpHeaders.ORIGIN, origin).exchange();

		assertThat(result).hasStatus(HttpStatus.OK).hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
		assertThat(result.getResponse().getHeaders(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
			.containsExactly(String.join(", ", HttpHeaders.RETRY_AFTER, ObservabilityConstants.REQUEST_ID_HEADER));
	}

	@Test
	void corsRejectsUnknownOrigin() {
		String origin = UNKNOWN_ORIGIN;

		MvcTestResult result = mvc.options()
			.uri(GrantProbeController.SCREEN_PATH)
			.header(HttpHeaders.ORIGIN, origin)
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
			.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN)
			.doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
	}

}
