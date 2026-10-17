package com.foody.api.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.auth.repository.RefreshTokenRepository;
import com.foody.api.me.MeConstants;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.security.Role;
import com.foody.api.shared.util.HashUtils;
import com.foody.api.user.UserConstants;
import com.foody.api.user.dto.UserStatus;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import com.foody.testsupport.security.AuthClient;
import com.foody.testsupport.security.TestCredentials;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class DisableUserTest {

	private static final String DISABLE_PATH = UserConstants.ADMIN_USERS_PATH + UserConstants.DISABLE_PATH;

	private static final String ENABLE_PATH = UserConstants.ADMIN_USERS_PATH + UserConstants.ENABLE_PATH;

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserFactory users;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokens;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AccessTokens tokens;

	@Autowired
	private AuthClient authClient;

	@Test
	void adminDisablesUser() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		User customer = users.create();

		MvcTestResult result = post(DISABLE_PATH, admin, customer.getId());

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(customer.getId().toString());
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(UserStatus.DISABLED.name());
		assertThat(result).bodyJson().extractingPath("$.disabledAt").asString().isNotBlank();
		assertThat(userRepository.findById(customer.getId())).map(User::isActive).contains(false);
	}

	@Test
	void disabledUsersAccessTokenIsRejected() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		User customer = users.create();
		String customerBearer = tokens.bearer(customer);
		post(DISABLE_PATH, admin, customer.getId());

		MvcTestResult result = mvc.get()
			.uri(MeConstants.PATH)
			.header(HttpHeaders.AUTHORIZATION, customerBearer)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
	}

	@Test
	void disablingRevokesRefreshTokens() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		User customer = users.create(user -> user.changePassword(passwordEncoder.encode(TestCredentials.PASSWORD)));
		String phoneToken = authClient.refreshTokenOf(authClient.login(customer.getEmail(), TestCredentials.PASSWORD));
		String laptopToken = authClient.refreshTokenOf(authClient.login(customer.getEmail(), TestCredentials.PASSWORD));
		post(DISABLE_PATH, admin, customer.getId());
		MvcTestResult refreshWhileDisabled = authClient.refresh(phoneToken);
		post(ENABLE_PATH, admin, customer.getId());

		MvcTestResult refreshAfterEnable = authClient.refresh(laptopToken);

		assertThat(refreshWhileDisabled).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(refreshAfterEnable).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(refreshAfterEnable).bodyJson()
			.extractingPath("$.code")
			.isEqualTo(Messages.REFRESH_TOKEN_INVALID.name());
		assertThat(List.of(phoneToken, laptopToken)).allSatisfy(
				token -> assertThat(refreshTokens.findByTokenHash(HashUtils.sha256Hex(token)).orElseThrow().isRevoked())
					.isTrue());
	}

	@Test
	void adminCannotDisableThemself() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));

		MvcTestResult result = post(DISABLE_PATH, admin, admin.getId());

		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.CANNOT_CHANGE_OWN_ACCOUNT.name());
		assertThat(userRepository.findById(admin.getId())).map(User::isActive).contains(true);
	}

	@Test
	void adminEnablesDisabledUser() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		User disabled = users.create(User::disable);

		MvcTestResult result = post(ENABLE_PATH, admin, disabled.getId());

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(UserStatus.ACTIVE.name());
		assertThat(result).bodyJson().extractingPath("$.disabledAt").isNull();
		assertThat(userRepository.findById(disabled.getId())).map(User::isActive).contains(true);
	}

	@Test
	void disableAndEnableAreIdempotent() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		User disabled = users.create(User::disable);
		User active = users.create();
		Instant firstDisabledAt = userRepository.findById(disabled.getId()).orElseThrow().getDisabledAt();

		MvcTestResult disableAgain = post(DISABLE_PATH, admin, disabled.getId());
		MvcTestResult enableAgain = post(ENABLE_PATH, admin, active.getId());

		assertThat(disableAgain).hasStatus(HttpStatus.OK);
		assertThat(disableAgain).bodyJson().extractingPath("$.status").isEqualTo(UserStatus.DISABLED.name());
		assertThat(userRepository.findById(disabled.getId())).map(User::getDisabledAt).contains(firstDisabledAt);
		assertThat(enableAgain).hasStatus(HttpStatus.OK);
		assertThat(enableAgain).bodyJson().extractingPath("$.status").isEqualTo(UserStatus.ACTIVE.name());
		assertThat(userRepository.findById(active.getId())).map(User::getVersion).contains(active.getVersion());
	}

	@Test
	void disablingAnUnknownUserAnswersNotFound() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		UUID unknownId = UUID.randomUUID();

		MvcTestResult result = post(DISABLE_PATH, admin, unknownId);

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.RESOURCE_NOT_FOUND.name());
	}

	private MvcTestResult post(String path, User caller, UUID userId) {
		return mvc.post().uri(path, userId).header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller)).exchange();
	}

}
