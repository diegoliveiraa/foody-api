package com.foody.api.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.persistence.ActivationStatus;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.seed.catalog.CategoryFactory;
import com.foody.seed.catalog.ProductFactory;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class ListProductsTest {

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
	void staffListsProductsPaginatedSortedByName() {
		User staff = staff();
		String marker = marker();
		Category category = categories.create();
		List.of("L", "C", "A", "J", "B", "I", "D", "H", "E", "G", "F")
			.forEach(letter -> products.create(category, marker + " " + letter, PRICE));
		Product last = products.create(category, marker + " K", new BigDecimal("34.90"));

		MvcTestResult result = mvc.get()
			.uri(CatalogConstants.PRODUCTS_PATH)
			.param("search", marker)
			.param("page", "2")
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(staff))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].name")
			.asArray()
			.containsExactly(marker + " K", marker + " L");
		assertThat(result).bodyJson().extractingPath("$.data[0].id").isEqualTo(last.getId().toString());
		assertThat(result).bodyJson().extractingPath("$.data[0].description").isEqualTo(last.getDescription());
		assertThat(result).bodyJson().extractingPath("$.data[0].price").isEqualTo(34.9);
		assertThat(result).bodyJson().extractingPath("$.data[0].category.id").isEqualTo(category.getId().toString());
		assertThat(result).bodyJson().extractingPath("$.data[0].category.name").isEqualTo(category.getName());
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
	void productsSortByNameInPortugueseOrder() {
		User staff = staff();
		String marker = marker();
		Category category = categories.create();
		List<String> portugueseOrder = Stream.of("Açaí na tigela", "Água com gás", "Anéis de cebola", "Batata frita")
			.map(name -> marker + " " + name)
			.toList();
		List.of(2, 3, 0, 1).forEach(index -> products.create(category, portugueseOrder.get(index), PRICE));

		MvcTestResult result = list(staff, "search", marker);

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].name")
			.asArray()
			.containsExactlyElementsOf(portugueseOrder);
	}

	@Test
	void productListFiltersByCategoryStatusAndSearch() {
		User staff = staff();
		String marker = marker();
		Category drinks = categories.create();
		Category other = categories.create();
		Product orange = products.create(drinks, marker + " Suco de laranja", PRICE);
		Product grape = products.create(drinks, marker + " Suco de uva", PRICE, Product::disable);
		Product water = products.create(drinks, marker + " Água", PRICE);
		products.create(other, marker + " Suco de limão", PRICE);

		MvcTestResult combined = list(staff, "categoryId", drinks.getId().toString(), "status",
				ActivationStatus.ACTIVE.name(), "search", "  SUCO ");
		MvcTestResult byCategory = list(staff, "categoryId", drinks.getId().toString());
		MvcTestResult byStatus = list(staff, "search", marker, "status", ActivationStatus.DISABLED.name());

		assertThat(combined).hasStatus(HttpStatus.OK);
		assertThat(combined).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(orange));
		assertThat(byCategory).bodyJson()
			.extractingPath("$.data[*].id")
			.asArray()
			.containsExactlyInAnyOrder(idOf(orange), idOf(grape), idOf(water));
		assertThat(byStatus).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(grape));
		assertThat(byStatus).bodyJson().extractingPath("$.data[0].status").isEqualTo(ActivationStatus.DISABLED.name());
		assertThat(byStatus).bodyJson().extractingPath("$.data[0].disabledAt").asString().isNotBlank();
	}

	@Test
	void productSearchIgnoresAccents() {
		User staff = staff();
		String marker = marker();
		Category category = categories.create();
		Product acai = products.create(category, marker + " Açaí na tigela", PRICE);
		products.create(category, marker + " Acarajé", PRICE);

		MvcTestResult plain = list(staff, "search", marker + " acai");
		MvcTestResult capitals = list(staff, "search", (marker + " ACAI").toUpperCase(Locale.ROOT));

		assertThat(plain).hasStatus(HttpStatus.OK);
		assertThat(plain).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(acai));
		assertThat(capitals).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(acai));
	}

	@Test
	void productSearchTreatsWildcardsAsText() {
		User staff = staff();
		String marker = marker();
		Category category = categories.create();
		Product discounted = products.create(category, marker + " Combo 100% frango", PRICE);
		products.create(category, marker + " Combo 1000 frango", PRICE);

		MvcTestResult result = list(staff, "search", (marker + " Combo 100%").toUpperCase(Locale.ROOT));

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(discounted));
	}

	@Test
	void productListSortsByPriceDescending() {
		User staff = staff();
		Category category = categories.create();
		Product cheap = products.create(category, marker() + " Água", new BigDecimal("5.00"));
		Product expensive = products.create(category, marker() + " Picanha", new BigDecimal("89.90"));
		Product middle = products.create(category, marker() + " Lanche", new BigDecimal("32.50"));

		MvcTestResult result = list(staff, "categoryId", category.getId().toString(), "sort", "price,desc");

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].id")
			.asArray()
			.containsExactly(idOf(expensive), idOf(middle), idOf(cheap));
	}

	@Test
	void productListRejectsUnknownSortProperty() {
		User staff = staff();

		MvcTestResult result = list(staff, "sort", "description,asc");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_SORT.name());
	}

	@Test
	void productListRejectsPageSizeOutsideTheAllowedSizes() {
		User staff = staff();

		MvcTestResult result = list(staff, "pageSize", "20");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_PAGE_SIZE.name());
	}

	private MvcTestResult list(User caller, String... parameters) {
		MockMvcTester.MockMvcRequestBuilder request = mvc.get()
			.uri(CatalogConstants.PRODUCTS_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller));
		for (int index = 0; index < parameters.length; index += 2) {
			request.param(parameters[index], parameters[index + 1]);
		}
		return request.exchange();
	}

	private User staff() {
		return users.create(user -> user.changeRole(Role.STAFF));
	}

	private String idOf(Product product) {
		return product.getId().toString();
	}

	private String marker() {
		return "prd" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

}
