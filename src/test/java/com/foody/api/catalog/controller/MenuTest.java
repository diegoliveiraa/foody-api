package com.foody.api.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.seed.catalog.CategoryFactory;
import com.foody.seed.catalog.ProductFactory;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class MenuTest {

	private static final BigDecimal PRICE = new BigDecimal("10.00");

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserFactory users;

	@Autowired
	private CategoryFactory categories;

	@Autowired
	private ProductFactory products;

	@Autowired
	private AccessTokens tokens;

	@Test
	void menuListsOnlyActiveCategoriesWithActiveProductsInOrder() {
		User customer = users.create();
		String marker = marker();
		Category drinks = categories.create(marker + " Bebidas", 20);
		Category burgers = categories.create(marker + " Lanches", 10);
		Category empty = categories.create(marker + " Vazia", 5);
		Category onlyDisabled = categories.create(marker + " Fora do ar", 15);
		Product smash = products.create(burgers, "Smash duplo", new BigDecimal("34.90"));
		Product chicken = products.create(burgers, "Frango crispy", new BigDecimal("29.90"));
		products.create(burgers, "Bauru", PRICE, Product::disable);
		Product lemonade = products.create(drinks, "Limonada da casa", new BigDecimal("12.00"));
		products.create(onlyDisabled, "Pastel", PRICE, Product::disable);
		Set<String> created = Stream.of(drinks, burgers, empty, onlyDisabled)
			.map(category -> category.getId().toString())
			.collect(Collectors.toSet());

		MvcTestResult result = mvc.get()
			.uri(CatalogConstants.MENU_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(customer))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.categories[*].id")
			.asArray()
			.filteredOn(created::contains)
			.containsExactly(burgers.getId().toString(), drinks.getId().toString());
		assertThat(result).bodyJson()
			.extractingPath(fieldOf(burgers, "name"))
			.asArray()
			.containsExactly(marker + " Lanches");
		assertThat(result).bodyJson()
			.extractingPath(fieldOf(burgers, "products[*].id"))
			.asArray()
			.containsExactly(chicken.getId().toString(), smash.getId().toString());
		assertThat(result).bodyJson()
			.extractingPath(fieldOf(burgers, "products[*].name"))
			.asArray()
			.containsExactly("Frango crispy", "Smash duplo");
		assertThat(result).bodyJson()
			.extractingPath(fieldOf(burgers, "products[*].price"))
			.asArray()
			.containsExactly(29.9, 34.9);
		assertThat(result).bodyJson()
			.extractingPath(fieldOf(drinks, "products[*].description"))
			.asArray()
			.containsExactly(lemonade.getDescription());
		assertThat(result).bodyJson()
			.extractingPath(fieldOf(drinks, "products[*].price"))
			.asArray()
			.containsExactly(12.0);
	}

	@Test
	void menuHidesProductsOfDisabledCategory() {
		User customer = users.create();
		Category pizzas = categories.create(marker() + " Pizzas", 10, Category::disable);
		Product margherita = products.create(pizzas, "Margherita", new BigDecimal("49.90"));

		MvcTestResult result = mvc.get()
			.uri(CatalogConstants.MENU_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(customer))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.categories[*].id")
			.asArray()
			.doesNotContain(pizzas.getId().toString());
		assertThat(result).bodyJson()
			.extractingPath("$.categories[*].products[*].id")
			.asArray()
			.doesNotContain(margherita.getId().toString());
	}

	@Test
	void staffWithoutStoreScreenCannotReadMenu() {
		User staff = users.create(user -> user.changeRole(Role.STAFF));

		MvcTestResult result = mvc.get()
			.uri(CatalogConstants.MENU_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(staff))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
	}

	@Test
	void anonymousCannotReadMenu() {
		String path = CatalogConstants.MENU_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
	}

	private String fieldOf(Category category, String field) {
		return "$.categories[?(@.id == '" + category.getId() + "')]." + field;
	}

	private String marker() {
		return "mnu" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

}
