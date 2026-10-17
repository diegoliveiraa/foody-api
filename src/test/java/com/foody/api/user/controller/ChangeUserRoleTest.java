package com.foody.api.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.me.MeConstants;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.security.Role;
import com.foody.api.user.UserConstants;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class ChangeUserRoleTest {

	private static final String ROLE_REQUEST = """
			{ "role": "%s" }
			""";

	private static final String EMPTY_REQUEST = "{}";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserFactory users;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AccessTokens tokens;

	@Test
	void adminChangesAnotherUsersRole() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		User customer = users.create();

		MvcTestResult result = changeRole(admin, customer.getId(), ROLE_REQUEST.formatted(Role.STAFF));

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(customer.getId().toString());
		assertThat(result).bodyJson().extractingPath("$.role").isEqualTo(Role.STAFF.name());
		assertThat(userRepository.findById(customer.getId())).map(User::getRole).contains(Role.STAFF);
	}

	@Test
	void roleChangeAppliesOnTheNextRequest() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		User customer = users.create();
		String customerBearer = tokens.bearer(customer);
		changeRole(admin, customer.getId(), ROLE_REQUEST.formatted(Role.STAFF));

		MvcTestResult result = mvc.get()
			.uri(MeConstants.PATH)
			.header(HttpHeaders.AUTHORIZATION, customerBearer)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.modules[*].key")
			.asArray()
			.containsExactly("dashboard", "orders", "catalog");
	}

	@Test
	void adminCannotChangeOwnRole() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));

		MvcTestResult result = changeRole(admin, admin.getId(), ROLE_REQUEST.formatted(Role.STAFF));

		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.CANNOT_CHANGE_OWN_ACCOUNT.name());
		assertThat(userRepository.findById(admin.getId())).map(User::getRole).contains(Role.ADMIN);
	}

	@Test
	void changingToTheSameRoleIsIdempotent() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		User staff = users.create(user -> user.changeRole(Role.STAFF));
		long versionBefore = staff.getVersion();

		MvcTestResult result = changeRole(admin, staff.getId(), ROLE_REQUEST.formatted(Role.STAFF));

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.role").isEqualTo(Role.STAFF.name());
		assertThat(userRepository.findById(staff.getId())).map(User::getVersion).contains(versionBefore);
	}

	@Test
	void roleChangeOfUnknownUserAnswersNotFound() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		UUID unknownId = UUID.randomUUID();

		MvcTestResult result = changeRole(admin, unknownId, ROLE_REQUEST.formatted(Role.STAFF));

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.RESOURCE_NOT_FOUND.name());
	}

	@Test
	void roleChangeWithoutARoleAnswersValidationFailed() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		User customer = users.create();

		MvcTestResult result = changeRole(admin, customer.getId(), EMPTY_REQUEST);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("role");
	}

	private MvcTestResult changeRole(User caller, UUID userId, String body) {
		return mvc.patch()
			.uri(UserConstants.ADMIN_USERS_PATH + UserConstants.ROLE_PATH, userId)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

}
