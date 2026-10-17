package com.foody.api.me.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.me.MeConstants;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class MeControllerTest {

	private static final String CUSTOMER_MODULES = """
			[
			  {
			    "key": "store",
			    "label": "Loja",
			    "screens": [
			      { "key": "store.menu", "label": "Cardápio", "actions": ["PLACE_ORDER"] },
			      { "key": "store.my-orders", "label": "Pedidos", "actions": ["CANCEL", "REQUEST_CANCELLATION"] }
			    ]
			  }
			]
			""";

	private static final String STAFF_MODULES = """
			[
			  {
			    "key": "dashboard",
			    "label": "Dashboard",
			    "screens": [ { "key": "dashboard.overview", "label": "Visão geral", "actions": [] } ]
			  },
			  {
			    "key": "orders",
			    "label": "Pedidos",
			    "screens": [
			      {
			        "key": "orders.order-list",
			        "label": "Pedidos",
			        "actions": ["UPDATE_STATUS", "CANCEL", "DECIDE_CANCELLATION"]
			      }
			    ]
			  },
			  {
			    "key": "catalog",
			    "label": "Catálogo",
			    "screens": [
			      { "key": "catalog.products", "label": "Produtos", "actions": ["CREATE", "UPDATE", "DISABLE", "DELETE"] },
			      { "key": "catalog.categories", "label": "Categorias", "actions": ["CREATE", "UPDATE", "DISABLE", "DELETE"] }
			    ]
			  }
			""";

	private static final String ADMIN_MODULE = """
			  ,
			  {
			    "key": "admin",
			    "label": "Administração",
			    "screens": [ { "key": "admin.users", "label": "Usuários", "actions": ["UPDATE", "DISABLE"] } ]
			  }
			""";

	private static final String END_OF_LIST = "]";

	private static final String BODY = """
			{ "user": { "id": "%s", "name": "%s", "email": "%s" }, "modules": %s }
			""";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserFactory users;

	@Autowired
	private AccessTokens tokens;

	@Test
	void meReturnsCustomerNavigation() {
		User customer = users.create();

		MvcTestResult result = mvc.get()
			.uri(MeConstants.PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(customer))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().isEqualTo(body(customer, CUSTOMER_MODULES), JsonCompareMode.STRICT);
	}

	@Test
	void meReturnsStaffNavigation() {
		User staff = users.create(user -> user.changeRole(Role.STAFF));

		MvcTestResult result = mvc.get()
			.uri(MeConstants.PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(staff))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().isEqualTo(body(staff, STAFF_MODULES + END_OF_LIST), JsonCompareMode.STRICT);
	}

	@Test
	void meReturnsAdminNavigationIncludingUsers() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));

		MvcTestResult result = mvc.get()
			.uri(MeConstants.PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(admin))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.isEqualTo(body(admin, STAFF_MODULES + ADMIN_MODULE + END_OF_LIST), JsonCompareMode.STRICT);
	}

	@Test
	void meNeverExposesTheRole() {
		User admin = users.create(user -> user.changeRole(Role.ADMIN));

		MvcTestResult result = mvc.get()
			.uri(MeConstants.PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(admin))
			.exchange();

		assertThat(result).bodyJson().extractingPath("$.user").asMap().containsOnlyKeys("id", "name", "email");
		assertThat(result).body().asString().doesNotContainIgnoringCase("\"role\"").doesNotContain(Role.ADMIN.name());
	}

	@Test
	void meWithoutTokenAnswersUnauthenticated() {
		String path = MeConstants.PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
	}

	private String body(User user, String modules) {
		return BODY.formatted(user.getId(), user.getName(), user.getEmail(), modules);
	}

}
