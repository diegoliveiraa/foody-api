package com.foody.api.user.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.shared.message.Messages;
import com.foody.api.shared.persistence.ActivationStatus;
import com.foody.api.shared.security.Role;
import com.foody.api.user.UserConstants;
import com.foody.api.user.entity.User;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class ListUsersTest {

	private static final String EMAIL_DOMAIN = "@foody.test";

	private static final List<String> LIKE_WILDCARDS_AND_LOOKALIKES = List.of("%", "_", "\\", "％", "﹪", "＿", "＼", "﹨",
			"∖");

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserFactory users;

	@Autowired
	private AccessTokens tokens;

	@Test
	void adminListsUsersPaginatedSortedByName() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String marker = marker();
		List.of("L", "C", "A", "K", "B", "J", "D", "I", "E", "H", "F", "G")
			.forEach(letter -> users.create(marker + " " + letter, uniqueEmail()));

		MvcTestResult result = mvc.get()
			.uri(UserConstants.ADMIN_USERS_PATH)
			.param("search", marker)
			.param("page", "2")
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(admin))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].name")
			.asArray()
			.containsExactly(marker + " K", marker + " L");
		assertThat(result).bodyJson().extractingPath("$.data[0].role").isEqualTo(Role.CUSTOMER.name());
		assertThat(result).bodyJson().extractingPath("$.data[0].status").isEqualTo(ActivationStatus.ACTIVE.name());
		assertThat(result).bodyJson().extractingPath("$.data[0].createdAt").asString().isNotBlank();
		assertThat(result).bodyJson().extractingPath("$.data[0].disabledAt").isNull();
		assertThat(result).bodyJson().extractingPath("$.meta.page").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.meta.pageSize").isEqualTo(10);
		assertThat(result).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(12);
		assertThat(result).bodyJson().extractingPath("$.meta.totalPages").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.meta.hasNext").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.meta.hasPrevious").isEqualTo(true);
	}

	@Test
	void usersSortByNameInPortugueseOrder() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String marker = marker();
		List<String> portugueseOrder = Stream.of("Álvaro", "Bruno", "Érica", "Fábio", "Ícaro", "Zeca")
			.map(name -> marker + " " + name)
			.toList();
		List.of(4, 1, 5, 0, 3, 2).forEach(index -> users.create(portugueseOrder.get(index), uniqueEmail()));

		MvcTestResult ascending = listSorted(admin, marker, "name,asc");
		MvcTestResult descending = listSorted(admin, marker, "name,desc");

		assertThat(ascending).hasStatus(HttpStatus.OK);
		assertThat(ascending).bodyJson()
			.extractingPath("$.data[*].name")
			.asArray()
			.containsExactlyElementsOf(portugueseOrder);
		assertThat(descending).hasStatus(HttpStatus.OK);
		assertThat(descending).bodyJson()
			.extractingPath("$.data[*].name")
			.asArray()
			.containsExactlyElementsOf(portugueseOrder.reversed());
	}

	@Test
	void listFiltersBySearchOnNameOrEmailIgnoringCase() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String marker = marker();
		User byName = users.create("Ana " + marker + " Souza", uniqueEmail());
		User byEmail = users.create("Bruno Lima", "contato." + marker + EMAIL_DOMAIN);
		users.create();

		MvcTestResult result = mvc.get()
			.uri(UserConstants.ADMIN_USERS_PATH)
			.param("search", "  " + marker.toUpperCase(Locale.ROOT) + " ")
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(admin))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].id")
			.asArray()
			.containsExactlyInAnyOrder(byName.getId().toString(), byEmail.getId().toString());
	}

	@Test
	void userSearchIgnoresAccentsAndCase() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String marker = marker();
		User joao = users.create("João " + marker + " Silva", uniqueEmail());
		User byEmail = users.create("Bruno Lima", "joao." + marker + EMAIL_DOMAIN);
		users.create("Joana " + marker + " Silva", uniqueEmail());

		MvcTestResult lowercase = listSearching(admin, "joao " + marker);
		MvcTestResult capitals = listSearching(admin, ("joao " + marker).toUpperCase(Locale.ROOT));
		MvcTestResult email = listSearching(admin, ("joão." + marker).toUpperCase(Locale.ROOT));

		assertThat(List.of(lowercase, capitals, email))
			.allSatisfy(result -> assertThat(result).hasStatus(HttpStatus.OK));
		assertThat(lowercase).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(joao));
		assertThat(capitals).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(joao));
		assertThat(email).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(byEmail));
	}

	@Test
	void searchStillEscapesLikeWildcards() throws UnsupportedEncodingException {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String marker = marker();
		List<String> searches = IntStream.range(0, LIKE_WILDCARDS_AND_LOOKALIKES.size())
			.mapToObj(index -> marker + index + " a" + LIKE_WILDCARDS_AND_LOOKALIKES.get(index) + "b")
			.toList();
		List<List<String>> expected = searches.stream()
			.map(search -> List.of(idOf(users.create(search, uniqueEmail()))))
			.toList();
		IntStream.range(0, searches.size())
			.forEach(index -> List.of(" axb", " ab")
				.forEach(decoy -> users.create(marker + index + decoy, uniqueEmail())));

		List<List<String>> found = new ArrayList<>();
		for (String search : searches) {
			found.add(JsonPath.read(listSearching(admin, search).getResponse().getContentAsString(), "$.data[*].id"));
		}

		assertThat(found).containsExactlyElementsOf(expected);
	}

	@Test
	void listFiltersByRole() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String marker = marker();
		users.create(marker + " Cliente", uniqueEmail());
		User staff = users.create(marker + " Equipe", uniqueEmail(), user -> user.changeRole(Role.STAFF));
		users.create(marker + " Admin", uniqueEmail(), user -> user.changeRole(Role.ADMIN));

		MvcTestResult result = mvc.get()
			.uri(UserConstants.ADMIN_USERS_PATH)
			.param("search", marker)
			.param("role", Role.STAFF.name())
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(admin))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].id")
			.asArray()
			.containsExactly(staff.getId().toString());
		assertThat(result).bodyJson().extractingPath("$.data[0].role").isEqualTo(Role.STAFF.name());
	}

	@Test
	void listFiltersByStatus() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));
		String marker = marker();
		users.create(marker + " Ativo", uniqueEmail());
		User disabled = users.create(marker + " Desativado", uniqueEmail(), User::disable);

		MvcTestResult result = mvc.get()
			.uri(UserConstants.ADMIN_USERS_PATH)
			.param("search", marker)
			.param("status", ActivationStatus.DISABLED.name())
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(admin))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].id")
			.asArray()
			.containsExactly(disabled.getId().toString());
		assertThat(result).bodyJson().extractingPath("$.data[0].status").isEqualTo(ActivationStatus.DISABLED.name());
		assertThat(result).bodyJson().extractingPath("$.data[0].disabledAt").asString().isNotBlank();
	}

	@Test
	void listRejectsUnknownSortProperty() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));

		MvcTestResult result = mvc.get()
			.uri(UserConstants.ADMIN_USERS_PATH)
			.param("sort", "passwordHash,asc")
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(admin))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_SORT.name());
	}

	@Test
	void listRejectsPageSizeOutsideTheAllowedSizes() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));

		MvcTestResult result = mvc.get()
			.uri(UserConstants.ADMIN_USERS_PATH)
			.param("pageSize", "20")
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(admin))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_PAGE_SIZE.name());
	}

	@Test
	void staffCannotListUsers() {
		User staff = users.create(user -> user.changeRole(Role.STAFF));

		MvcTestResult result = mvc.get()
			.uri(UserConstants.ADMIN_USERS_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(staff))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
	}

	@Test
	void customerCannotListUsers() {
		User customer = users.create();

		MvcTestResult result = mvc.get()
			.uri(UserConstants.ADMIN_USERS_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(customer))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
	}

	@Test
	void anonymousCannotListUsers() {
		String path = UserConstants.ADMIN_USERS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
	}

	private MvcTestResult listSearching(User caller, String search) {
		return mvc.get()
			.uri(UserConstants.ADMIN_USERS_PATH)
			.param("search", search)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.exchange();
	}

	private String idOf(User user) {
		return user.getId().toString();
	}

	private MvcTestResult listSorted(User caller, String search, String sort) {
		return mvc.get()
			.uri(UserConstants.ADMIN_USERS_PATH)
			.param("search", search)
			.param("sort", sort)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.exchange();
	}

	private String marker() {
		return "usr" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

	private String uniqueEmail() {
		return "user." + UUID.randomUUID() + EMAIL_DOMAIN;
	}

}
