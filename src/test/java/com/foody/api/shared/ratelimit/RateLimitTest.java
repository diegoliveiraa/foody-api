package com.foody.api.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.me.MeConstants;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.observability.ObservabilityConstants;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.user.entity.User;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.api.RemoteAddress;
import com.foody.testsupport.security.AccessTokens;
import com.foody.testsupport.security.AuthClient;
import com.foody.testsupport.security.TestCredentials;
import com.foody.testsupport.time.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
@TestPropertySource(properties = { "foody.rate-limit.login.capacity=4", "foody.rate-limit.login-per-email.capacity=2",
		"foody.rate-limit.register.capacity=2", "foody.rate-limit.refresh.capacity=2",
		"foody.rate-limit.place-order.capacity=1", "foody.rate-limit.request-cancellation.capacity=1",
		"foody.rate-limit.authenticated.capacity=3", "foody.rate-limit.anonymous.capacity=3" })
class RateLimitTest {

	private static final int ANONYMOUS_CAPACITY = 3;

	private static final int AUTHENTICATED_CAPACITY = 3;

	private static final String WRONG_PASSWORD = "outra-senha-456";

	private static final String REGISTER_REQUEST = """
			{ "name": "Rita Limite", "email": "%s", "password": "%s" }
			""";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private AuthClient authClient;

	@Autowired
	private UserFactory users;

	@Autowired
	private AccessTokens tokens;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MutableClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void loginIsLimitedPerEmailAndIp() {
		String email = customerWithPassword().getEmail();
		authClient.loginFrom("203.0.113.1", email, WRONG_PASSWORD);
		authClient.loginFrom("203.0.113.1", email, WRONG_PASSWORD);

		MvcTestResult sameEmailSameIp = authClient.loginFrom("203.0.113.1", email, TestCredentials.PASSWORD);
		MvcTestResult sameEmailOtherIp = authClient.loginFrom("203.0.113.2", email, TestCredentials.PASSWORD);
		MvcTestResult otherEmailSameIp = authClient.loginFrom("203.0.113.1", "other.limit@foody.dev",
				TestCredentials.PASSWORD);

		assertThat(sameEmailSameIp).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(sameEmailSameIp).bodyJson().extractingPath("$.code").isEqualTo(Messages.RATE_LIMITED.name());
		assertThat(sameEmailOtherIp).hasStatus(HttpStatus.OK);
		assertThat(otherEmailSameIp).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void loginIsLimitedPerIp() {
		IntStream.range(0, 4)
			.forEach(attempt -> authClient.loginFrom("203.0.113.3", "spray" + attempt + "@foody.dev",
					TestCredentials.PASSWORD));

		MvcTestResult fifthEmail = authClient.loginFrom("203.0.113.3", "spray.last@foody.dev",
				TestCredentials.PASSWORD);
		MvcTestResult otherIp = authClient.loginFrom("203.0.113.4", "spray.last@foody.dev", TestCredentials.PASSWORD);

		assertThat(fifthEmail).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(otherIp).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void registerIsLimitedPerIp() {
		register("203.0.113.5", "first.limit.register@foody.dev");
		register("203.0.113.5", "second.limit.register@foody.dev");

		MvcTestResult third = register("203.0.113.5", "third.limit.register@foody.dev");
		MvcTestResult otherIp = register("203.0.113.6", "fourth.limit.register@foody.dev");

		assertThat(third).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(otherIp).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void refreshIsLimitedPerIp() {
		String noToken = null;
		authClient.refreshFrom("203.0.113.7", noToken);
		authClient.refreshFrom("203.0.113.7", noToken);

		MvcTestResult third = authClient.refreshFrom("203.0.113.7", noToken);
		MvcTestResult otherIp = authClient.refreshFrom("203.0.113.8", noToken);

		assertThat(third).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(otherIp).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(otherIp).bodyJson().extractingPath("$.code").isEqualTo(Messages.REFRESH_TOKEN_INVALID.name());
	}

	@Test
	void authenticatedRequestsAreLimitedPerUser() {
		User customer = users.create();
		List<String> addresses = List.of("198.51.100.1", "198.51.100.2", "198.51.100.3");

		List<MvcTestResult> withinLimit = addresses.stream().map(address -> me(customer, address)).toList();
		MvcTestResult beyondLimit = me(customer, "198.51.100.4");

		assertThat(withinLimit).allSatisfy(result -> assertThat(result).hasStatus(HttpStatus.OK));
		assertThat(beyondLimit).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
	}

	@Test
	void anonymousRequestsAreLimitedPerIp() {
		IntStream.range(0, ANONYMOUS_CAPACITY).forEach(attempt -> anonymousMe("203.0.113.9"));

		MvcTestResult beyondLimit = anonymousMe("203.0.113.9");
		MvcTestResult otherIp = anonymousMe("203.0.113.10");

		assertThat(beyondLimit).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(otherIp).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void healthIsNeverLimited() {
		String address = "203.0.113.11";

		List<MvcTestResult> results = IntStream.range(0, ANONYMOUS_CAPACITY * 3)
			.mapToObj(attempt -> mvc.get().uri("/actuator/health").with(RemoteAddress.of(address)).exchange())
			.toList();

		assertThat(results).allSatisfy(result -> assertThat(result).hasStatus(HttpStatus.OK));
	}

	@Test
	void limitedResponseCarriesRetryAfterAndProblemDetail() {
		clock.fixAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		IntStream.range(0, ANONYMOUS_CAPACITY).forEach(attempt -> anonymousMe("203.0.113.12"));

		MvcTestResult result = mvc.get()
			.uri(MeConstants.PATH)
			.with(RemoteAddress.of("203.0.113.12"))
			.header(ObservabilityConstants.REQUEST_ID_HEADER, "too-fast-1")
			.exchange();

		assertThat(result).hasStatus(HttpStatus.TOO_MANY_REQUESTS)
			.hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
			.hasHeader(HttpHeaders.RETRY_AFTER, "20")
			.hasHeader(ObservabilityConstants.REQUEST_ID_HEADER, "too-fast-1");
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(429);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.RATE_LIMITED.name());
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo(Messages.RATE_LIMITED.text());
		assertThat(result).bodyJson().extractingPath("$.requestId").isEqualTo("too-fast-1");
	}

	@Test
	void bucketRefillsAfterThePeriod() {
		clock.fixAt(Instant.now().truncatedTo(ChronoUnit.SECONDS));
		IntStream.range(0, ANONYMOUS_CAPACITY).forEach(attempt -> anonymousMe("203.0.113.13"));

		MvcTestResult beforeRefill = anonymousMe("203.0.113.13");
		clock.advance(Duration.ofMinutes(1));
		List<MvcTestResult> afterRefill = IntStream.range(0, ANONYMOUS_CAPACITY)
			.mapToObj(attempt -> anonymousMe("203.0.113.13"))
			.toList();

		assertThat(beforeRefill).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(afterRefill).allSatisfy(result -> assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED));
	}

	@Test
	void oneUserExhaustingTheirBucketDoesNotAffectAnother() {
		User greedy = users.create();
		User patient = users.create();
		IntStream.range(0, AUTHENTICATED_CAPACITY).forEach(attempt -> me(greedy, "203.0.113.14"));

		MvcTestResult greedyAgain = me(greedy, "203.0.113.14");
		MvcTestResult patientFromTheSameIp = me(patient, "203.0.113.14");

		assertThat(greedyAgain).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(patientFromTheSameIp).hasStatus(HttpStatus.OK);
	}

	@Test
	void forwardedForHeaderIsIgnored() {
		IntStream.range(0, ANONYMOUS_CAPACITY)
			.forEach(attempt -> anonymousMeForwardedFor("203.0.113.15", "192.0.2." + attempt));

		MvcTestResult spoofed = anonymousMeForwardedFor("203.0.113.15", "192.0.2.200");

		assertThat(spoofed).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
	}

	@Test
	void orderRoutesHaveTheirOwnPerUserLimits() {
		User customer = users.create();
		String cancellationPath = "/my-orders/" + UUID.randomUUID() + "/cancellation-request";
		post(customer, "/my-orders");
		post(customer, cancellationPath);

		MvcTestResult secondOrder = post(customer, "/my-orders");
		MvcTestResult secondCancellation = post(customer, cancellationPath);
		MvcTestResult otherRoute = me(customer, "203.0.113.16");

		assertThat(secondOrder).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(secondCancellation).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(otherRoute).hasStatus(HttpStatus.OK);
	}

	private User customerWithPassword() {
		return users.create(user -> user.changePassword(passwordEncoder.encode(TestCredentials.PASSWORD)));
	}

	private MvcTestResult register(String address, String email) {
		return mvc.post()
			.uri(RouteConstants.REGISTER_PATH)
			.with(RemoteAddress.of(address))
			.contentType(MediaType.APPLICATION_JSON)
			.content(REGISTER_REQUEST.formatted(email, TestCredentials.PASSWORD))
			.exchange();
	}

	private MvcTestResult me(User user, String address) {
		return mvc.get()
			.uri(MeConstants.PATH)
			.with(RemoteAddress.of(address))
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(user))
			.exchange();
	}

	private MvcTestResult anonymousMe(String address) {
		return mvc.get().uri(MeConstants.PATH).with(RemoteAddress.of(address)).exchange();
	}

	private MvcTestResult anonymousMeForwardedFor(String address, String forwardedFor) {
		return mvc.get()
			.uri(MeConstants.PATH)
			.with(RemoteAddress.of(address))
			.header("X-Forwarded-For", forwardedFor)
			.exchange();
	}

	private MvcTestResult post(User user, String path) {
		return mvc.post()
			.uri(path)
			.with(RemoteAddress.of("203.0.113.16"))
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(user))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")
			.exchange();
	}

}
