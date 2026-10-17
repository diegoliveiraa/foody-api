package com.foody.api.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.auth.AuthConstants;
import com.foody.api.me.MeConstants;
import com.foody.api.shared.exception.ErrorConstants;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.security.Role;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import com.foody.testsupport.security.AuthClient;
import com.foody.testsupport.security.TestCredentials;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class RegisterTest {

	private static final String COOKIE = AuthConstants.REFRESH_COOKIE_NAME;

	private static final String REQUEST = """
			{ "name": "%s", "email": "%s", "password": "%s" }
			""";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserFactory users;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuthClient authClient;

	@Autowired
	private AccessTokens tokens;

	@Test
	void registerCreatesCustomerAndReturnsAccessTokenWithRefreshCookie() {
		String body = REQUEST.formatted("  Ana Souza ", " Ana.Souza.Register@Foody.DEV ", TestCredentials.PASSWORD);

		MvcTestResult result = register(body);

		assertThat(result).hasStatus(HttpStatus.CREATED).hasContentType(MediaType.APPLICATION_JSON);
		assertThat(result).bodyJson().extractingPath("$.accessToken").asString().isNotBlank();
		assertThat(result).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
		assertThat(result).cookies()
			.containsCookie(COOKIE)
			.isHttpOnly(COOKIE, true)
			.isSecure(COOKIE, true)
			.hasPath(COOKIE, RouteConstants.AUTH_PATH)
			.hasMaxAge(COOKIE, Duration.ofDays(7));
		assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("SameSite=Strict");
		User created = userRepository.findByEmail("ana.souza.register@foody.dev").orElseThrow();
		assertThat(created.getName()).isEqualTo("Ana Souza");
		assertThat(created.getRole()).isEqualTo(Role.CUSTOMER);
		assertThat(created.isActive()).isTrue();
		assertThat(passwordEncoder.matches(TestCredentials.PASSWORD, created.getPasswordHash())).isTrue();
	}

	@Test
	void registerLogsTheNewCustomerInWithAWorkingAccessToken() {
		MvcTestResult registered = register(
				REQUEST.formatted("Bruno Lima", "bruno.register@foody.dev", TestCredentials.PASSWORD));
		String accessToken = authClient.accessTokenOf(registered);

		MvcTestResult me = mvc.get()
			.uri(MeConstants.PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(accessToken))
			.exchange();

		assertThat(me).hasStatus(HttpStatus.OK);
		assertThat(me).bodyJson().extractingPath("$.user.email").isEqualTo("bruno.register@foody.dev");
		assertThat(me).bodyJson().extractingPath("$.modules[0].key").isEqualTo("store");
	}

	@Test
	void registerRejectsEmailAlreadyRegisteredIgnoringCase() {
		User existing = users.create();
		String body = REQUEST.formatted("Carla Dias", existing.getEmail().toUpperCase(), TestCredentials.PASSWORD);

		MvcTestResult result = register(body);

		assertThat(result).hasStatus(HttpStatus.CONFLICT).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.EMAIL_ALREADY_REGISTERED.name());
		assertThat(result).cookies().doesNotContainCookie(COOKIE);
	}

	@Test
	void registerRejectsPasswordShorterThanEightCharacters() {
		String body = REQUEST.formatted("Davi Rocha", "davi.register@foody.dev", "1234567");

		MvcTestResult result = register(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("password");
		assertThat(result).bodyJson()
			.extractingPath("$.errors[0].message")
			.isEqualTo(Messages.PASSWORD_TOO_SHORT.text() + ErrorConstants.DETAIL_SEPARATOR
					+ AuthConstants.PASSWORD_MIN_LENGTH);
		assertThat(userRepository.existsByEmail("davi.register@foody.dev")).isFalse();
	}

	@Test
	void registerRejectsPasswordLongerThanSeventyTwoBytes() {
		String thirtySevenTwoByteCharacters = "ç".repeat(37);
		String body = REQUEST.formatted("Elisa Prado", "elisa.register@foody.dev", thirtySevenTwoByteCharacters);

		MvcTestResult result = register(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("password");
		assertThat(result).bodyJson()
			.extractingPath("$.errors[0].message")
			.isEqualTo(Messages.PASSWORD_TOO_LONG.text() + ErrorConstants.DETAIL_SEPARATOR
					+ AuthConstants.PASSWORD_MAX_BYTES);
	}

	@Test
	void registerAcceptsPasswordOfExactlySeventyTwoBytes() {
		String thirtySixTwoByteCharacters = "ç".repeat(36);
		String body = REQUEST.formatted("Fábio Reis", "fabio.register@foody.dev", thirtySixTwoByteCharacters);

		MvcTestResult result = register(body);

		assertThat(result).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void registerRejectsNameShorterThanTwoCharactersOnceTrimmed() {
		String body = REQUEST.formatted("  G  ", "gabriel.register@foody.dev", TestCredentials.PASSWORD);

		MvcTestResult result = register(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("name");
	}

	@Test
	void registerRejectsAMalformedEmail() {
		String body = REQUEST.formatted("Helena Melo", "helena-at-foody.dev", TestCredentials.PASSWORD);

		MvcTestResult result = register(body);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("email");
	}

	private MvcTestResult register(String body) {
		return mvc.post()
			.uri(RouteConstants.REGISTER_PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

}
