package com.foody.api.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.repository.CategoryRepository;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.persistence.ActivationStatus;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.seed.catalog.CategoryFactory;
import com.foody.seed.catalog.ProductFactory;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class ManageCategoriesTest {

	private static final String CATEGORY_PATH = CatalogConstants.CATEGORIES_PATH + RouteConstants.ID_PATH;

	private static final String DISABLE_PATH = CatalogConstants.CATEGORIES_PATH + RouteConstants.DISABLE_PATH;

	private static final String ENABLE_PATH = CatalogConstants.CATEGORIES_PATH + RouteConstants.ENABLE_PATH;

	private static final String CATEGORY_REQUEST = """
			{ "name": "%s", "displayOrder": %d }
			""";

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserFactory users;

	@Autowired
	private CategoryFactory categories;

	@Autowired
	private ProductFactory products;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private AccessTokens tokens;

	@Test
	void staffListsCategoriesInDisplayOrderWithProductCounts() {
		User staff = staff();
		String marker = marker();
		Category drinks = categories.create(marker + " Bebidas", 30);
		Category burgers = categories.create(marker + " Lanches", 10);
		Category pizzas = categories.create(marker + " Pizzas", 10);
		Category desserts = categories.create(marker + " Sobremesas", 20, Category::disable);
		products.create(burgers);
		products.create(burgers);
		products.create(burgers, Product::disable);
		products.create(drinks);
		Set<String> created = Stream.of(drinks, burgers, pizzas, desserts).map(this::idOf).collect(Collectors.toSet());

		MvcTestResult result = mvc.get()
			.uri(CatalogConstants.CATEGORIES_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(staff))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$[*].id")
			.asArray()
			.filteredOn(created::contains)
			.containsExactly(idOf(burgers), idOf(pizzas), idOf(desserts), idOf(drinks));
		assertThat(result).bodyJson().extractingPath(fieldOf(burgers, "productCount")).asArray().containsExactly(3);
		assertThat(result).bodyJson().extractingPath(fieldOf(pizzas, "productCount")).asArray().containsExactly(0);
		assertThat(result).bodyJson().extractingPath(fieldOf(drinks, "productCount")).asArray().containsExactly(1);
		assertThat(result).bodyJson()
			.extractingPath(fieldOf(burgers, "name"))
			.asArray()
			.containsExactly(marker + " Lanches");
		assertThat(result).bodyJson().extractingPath(fieldOf(burgers, "displayOrder")).asArray().containsExactly(10);
		assertThat(result).bodyJson()
			.extractingPath(fieldOf(burgers, "status"))
			.asArray()
			.containsExactly(ActivationStatus.ACTIVE.name());
		assertThat(result).bodyJson().extractingPath(fieldOf(burgers, "disabledAt")).asArray().containsOnlyNulls();
		assertThat(result).bodyJson()
			.extractingPath(fieldOf(desserts, "status"))
			.asArray()
			.containsExactly(ActivationStatus.DISABLED.name());
		assertThat(result).bodyJson().extractingPath(fieldOf(desserts, "disabledAt")).asArray().doesNotContainNull();
	}

	@Test
	void staffCreatesCategory() {
		User staff = staff();
		String name = marker() + " Bebidas";

		MvcTestResult result = send(staff, mvc.post().uri(CatalogConstants.CATEGORIES_PATH),
				CATEGORY_REQUEST.formatted("  " + name + "  ", 40));

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.id").asString().isNotBlank();
		assertThat(result).bodyJson().extractingPath("$.name").isEqualTo(name);
		assertThat(result).bodyJson().extractingPath("$.displayOrder").isEqualTo(40);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(ActivationStatus.ACTIVE.name());
		assertThat(result).bodyJson().extractingPath("$.productCount").isEqualTo(0);
		assertThat(result).bodyJson().extractingPath("$.disabledAt").isNull();
		assertThat(categoryRepository.findAll()).extracting(Category::getName).contains(name);
	}

	@Test
	void creatingCategoryWithTakenNameIgnoringCaseIsRejected() {
		User staff = staff();
		String name = marker() + " Bebidas";
		categories.create(name, 10);

		MvcTestResult result = send(staff, mvc.post().uri(CatalogConstants.CATEGORIES_PATH),
				CATEGORY_REQUEST.formatted(name.toUpperCase(Locale.ROOT), 20));

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.CATEGORY_NAME_TAKEN.name());
		assertThat(categoryRepository.findAll()).filteredOn(category -> category.getName().equalsIgnoreCase(name))
			.hasSize(1);
	}

	@Test
	void creatingCategoryRejectsInvalidFields() {
		User staff = staff();

		MvcTestResult result = send(staff, mvc.post().uri(CatalogConstants.CATEGORIES_PATH),
				CATEGORY_REQUEST.formatted("  a  ", 1000));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		assertThat(result).bodyJson()
			.extractingPath("$.errors[*].field")
			.asArray()
			.containsExactlyInAnyOrder("name", "displayOrder");
	}

	@Test
	void staffRenamesCategory() {
		User staff = staff();
		String marker = marker();
		Category category = categories.create(marker + " Bebidas", 10);

		MvcTestResult result = send(staff, mvc.put().uri(CATEGORY_PATH, category.getId()),
				CATEGORY_REQUEST.formatted(marker + " Drinks", 15));

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(idOf(category));
		assertThat(result).bodyJson().extractingPath("$.name").isEqualTo(marker + " Drinks");
		assertThat(result).bodyJson().extractingPath("$.displayOrder").isEqualTo(15);
		assertThat(categoryRepository.findById(category.getId())).map(Category::getName).contains(marker + " Drinks");
		assertThat(categoryRepository.findById(category.getId())).map(Category::getDisplayOrder).contains(15);
	}

	@Test
	void renamingCategoryKeepingItsOwnNameInAnotherCaseIsAllowed() {
		User staff = staff();
		String name = marker() + " bebidas";
		Category category = categories.create(name, 10);

		MvcTestResult result = send(staff, mvc.put().uri(CATEGORY_PATH, category.getId()),
				CATEGORY_REQUEST.formatted(name.toUpperCase(Locale.ROOT), 10));

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.name").isEqualTo(name.toUpperCase(Locale.ROOT));
	}

	@Test
	void renamingCategoryToAnotherCategorysNameIsRejected() {
		User staff = staff();
		String marker = marker();
		Category taken = categories.create(marker + " Bebidas", 10);
		Category category = categories.create(marker + " Drinks", 20);

		MvcTestResult result = send(staff, mvc.put().uri(CATEGORY_PATH, category.getId()),
				CATEGORY_REQUEST.formatted(taken.getName().toLowerCase(Locale.ROOT), 20));

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.CATEGORY_NAME_TAKEN.name());
		assertThat(categoryRepository.findById(category.getId())).map(Category::getName).contains(marker + " Drinks");
	}

	@Test
	void updatingUnknownCategoryIsRejected() {
		User staff = staff();

		MvcTestResult result = send(staff, mvc.put().uri(CATEGORY_PATH, UUID.randomUUID()),
				CATEGORY_REQUEST.formatted(marker() + " Bebidas", 10));

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.CATEGORY_NOT_FOUND.name());
	}

	@Test
	void staffDisablesAndEnablesCategory() throws UnsupportedEncodingException {
		User staff = staff();
		Category category = categories.create();

		MvcTestResult disabled = post(DISABLE_PATH, staff, category.getId());
		MvcTestResult disabledAgain = post(DISABLE_PATH, staff, category.getId());
		MvcTestResult enabled = post(ENABLE_PATH, staff, category.getId());

		assertThat(disabled).hasStatus(HttpStatus.OK);
		assertThat(disabled).bodyJson().extractingPath("$.status").isEqualTo(ActivationStatus.DISABLED.name());
		assertThat(disabled).bodyJson().extractingPath("$.disabledAt").asString().isNotBlank();
		assertThat(disabledAgain).hasStatus(HttpStatus.OK);
		assertThat(disabledAgain).bodyJson()
			.extractingPath("$.disabledAt")
			.isEqualTo(JsonPath.read(disabled.getResponse().getContentAsString(), "$.disabledAt"));
		assertThat(enabled).hasStatus(HttpStatus.OK);
		assertThat(enabled).bodyJson().extractingPath("$.status").isEqualTo(ActivationStatus.ACTIVE.name());
		assertThat(enabled).bodyJson().extractingPath("$.disabledAt").isNull();
		assertThat(categoryRepository.findById(category.getId())).map(Category::isActive).contains(true);
	}

	@Test
	void disablingUnknownCategoryIsRejected() {
		User staff = staff();

		MvcTestResult result = post(DISABLE_PATH, staff, UUID.randomUUID());

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.CATEGORY_NOT_FOUND.name());
	}

	@Test
	void deletingCategoryWithProductsIsRejected() {
		User staff = staff();
		Category category = categories.create();
		products.create(category, Product::disable);

		MvcTestResult result = delete(staff, category.getId());

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.CATEGORY_IN_USE.name());
		assertThat(categoryRepository.existsById(category.getId())).isTrue();
	}

	@Test
	void staffDeletesEmptyCategory() {
		User staff = staff();
		Category category = categories.create();

		MvcTestResult result = delete(staff, category.getId());

		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(categoryRepository.existsById(category.getId())).isFalse();
	}

	@Test
	void deletingUnknownCategoryIsRejected() {
		User staff = staff();

		MvcTestResult result = delete(staff, UUID.randomUUID());

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.CATEGORY_NOT_FOUND.name());
	}

	@Test
	void customerCannotManageCategories() {
		User customer = users.create();
		Category category = categories.create();
		String request = CATEGORY_REQUEST.formatted(marker() + " Bebidas", 10);

		List<MvcTestResult> results = List.of(
				mvc.get()
					.uri(CatalogConstants.CATEGORIES_PATH)
					.header(HttpHeaders.AUTHORIZATION, tokens.bearer(customer))
					.exchange(),
				send(customer, mvc.post().uri(CatalogConstants.CATEGORIES_PATH), request),
				send(customer, mvc.put().uri(CATEGORY_PATH, category.getId()), request),
				post(DISABLE_PATH, customer, category.getId()), post(ENABLE_PATH, customer, category.getId()),
				delete(customer, category.getId()));

		assertThat(results).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
		});
		assertThat(categoryRepository.findById(category.getId())).map(Category::isActive).contains(true);
	}

	@Test
	void anonymousCannotListCategories() {
		String path = CatalogConstants.CATEGORIES_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
	}

	private MvcTestResult send(User caller, MockMvcTester.MockMvcRequestBuilder request, String body) {
		return request.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	private MvcTestResult post(String path, User caller, UUID id) {
		return mvc.post().uri(path, id).header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller)).exchange();
	}

	private MvcTestResult delete(User caller, UUID id) {
		return mvc.delete().uri(CATEGORY_PATH, id).header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller)).exchange();
	}

	private User staff() {
		return users.create(user -> user.changeRole(Role.STAFF));
	}

	private String idOf(Category category) {
		return category.getId().toString();
	}

	private String fieldOf(Category category, String field) {
		return "$[?(@.id == '" + category.getId() + "')]." + field;
	}

	private String marker() {
		return "cat" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

}
