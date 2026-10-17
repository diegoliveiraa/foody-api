package com.foody.api.catalog.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.repository.ProductRepository;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.persistence.ActivationStatus;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.seed.catalog.CategoryFactory;
import com.foody.seed.catalog.ProductFactory;
import com.foody.seed.order.OrderFactory;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionOperations;

@ApiIntegrationTest
class ManageProductsTest {

	private static final String PRODUCT_PATH = CatalogConstants.PRODUCTS_PATH + RouteConstants.ID_PATH;

	private static final String DISABLE_PATH = CatalogConstants.PRODUCTS_PATH + RouteConstants.DISABLE_PATH;

	private static final String ENABLE_PATH = CatalogConstants.PRODUCTS_PATH + RouteConstants.ENABLE_PATH;

	private static final String PRODUCT_REQUEST = """
			{ "name": "%s", "description": "%s", "price": %s, "categoryId": "%s" }
			""";

	private static final String PRODUCT_REQUEST_WITHOUT_DESCRIPTION = """
			{ "name": "%s", "price": %s, "categoryId": "%s" }
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
	private OrderFactory orders;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private TransactionOperations transactions;

	@Autowired
	private AccessTokens tokens;

	@Test
	void staffCreatesProduct() {
		User staff = staff();
		Category category = categories.create();
		String name = marker() + " X-burger";

		MvcTestResult result = send(staff, mvc.post().uri(CatalogConstants.PRODUCTS_PATH),
				PRODUCT_REQUEST.formatted("  " + name + " ", "  Pão, carne e queijo  ", "32.90", category.getId()));

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.id").asString().isNotBlank();
		assertThat(result).bodyJson().extractingPath("$.name").isEqualTo(name);
		assertThat(result).bodyJson().extractingPath("$.description").isEqualTo("Pão, carne e queijo");
		assertThat(result).bodyJson().extractingPath("$.price").isEqualTo(32.9);
		assertThat(result).bodyJson().extractingPath("$.category.id").isEqualTo(category.getId().toString());
		assertThat(result).bodyJson().extractingPath("$.category.name").isEqualTo(category.getName());
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(ActivationStatus.ACTIVE.name());
		assertThat(result).bodyJson().extractingPath("$.createdAt").asString().isNotBlank();
		assertThat(result).bodyJson().extractingPath("$.disabledAt").isNull();
		assertThat(productNamed(name)).singleElement()
			.satisfies(product -> assertThat(product.getPrice()).isEqualByComparingTo("32.90"));
	}

	@Test
	void creatingProductWithoutDescriptionLeavesItEmpty() {
		User staff = staff();
		Category category = categories.create();
		String name = marker() + " Água";

		MvcTestResult result = send(staff, mvc.post().uri(CatalogConstants.PRODUCTS_PATH),
				PRODUCT_REQUEST_WITHOUT_DESCRIPTION.formatted(name, "9999.99", category.getId()));

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.description").isNull();
		assertThat(result).bodyJson().extractingPath("$.price").isEqualTo(9999.99);
	}

	@Test
	void creatingProductWithUnknownCategoryIsRejected() {
		User staff = staff();
		String name = marker() + " X-burger";

		MvcTestResult result = send(staff, mvc.post().uri(CatalogConstants.PRODUCTS_PATH),
				PRODUCT_REQUEST_WITHOUT_DESCRIPTION.formatted(name, "32.90", UUID.randomUUID()));

		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNKNOWN_CATEGORY.name());
		assertThat(productNamed(name)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "0", "-5.00", "10.999", "10000.00" })
	void creatingProductRejectsInvalidPrice(String price) {
		User staff = staff();
		Category category = categories.create();
		String name = marker() + " X-burger";

		MvcTestResult result = send(staff, mvc.post().uri(CatalogConstants.PRODUCTS_PATH),
				PRODUCT_REQUEST_WITHOUT_DESCRIPTION.formatted(name, price, category.getId()));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray().containsOnly("price");
		assertThat(productNamed(name)).isEmpty();
	}

	@Test
	void creatingProductRejectsInvalidFields() {
		User staff = staff();

		MvcTestResult result = send(staff, mvc.post().uri(CatalogConstants.PRODUCTS_PATH),
				PRODUCT_REQUEST.formatted(" x ", "a".repeat(301), "10.00", "null").replace("\"null\"", "null"));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		assertThat(result).bodyJson()
			.extractingPath("$.errors[*].field")
			.asArray()
			.containsExactlyInAnyOrder("name", "description", "categoryId");
	}

	@Test
	void staffUpdatesProduct() {
		User staff = staff();
		Category from = categories.create();
		Category to = categories.create();
		Product product = products.create(from);
		String name = marker() + " Novo nome";

		MvcTestResult result = send(staff, mvc.put().uri(PRODUCT_PATH, product.getId()),
				PRODUCT_REQUEST.formatted(name, "   ", "15.50", to.getId()));

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(product.getId().toString());
		assertThat(result).bodyJson().extractingPath("$.name").isEqualTo(name);
		assertThat(result).bodyJson().extractingPath("$.description").isNull();
		assertThat(result).bodyJson().extractingPath("$.price").isEqualTo(15.5);
		assertThat(result).bodyJson().extractingPath("$.category.id").isEqualTo(to.getId().toString());
		assertThat(result).bodyJson().extractingPath("$.category.name").isEqualTo(to.getName());
		assertThat(categoryIdOf(product.getId())).isEqualTo(to.getId());
		assertThat(productRepository.findById(product.getId())).map(Product::getPrice)
			.hasValueSatisfying(price -> assertThat(price).isEqualByComparingTo("15.50"));
	}

	@Test
	void updatingUnknownProductIsRejected() {
		User staff = staff();
		Category category = categories.create();

		MvcTestResult result = send(staff, mvc.put().uri(PRODUCT_PATH, UUID.randomUUID()),
				PRODUCT_REQUEST_WITHOUT_DESCRIPTION.formatted(marker() + " Novo", "10.00", category.getId()));

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.PRODUCT_NOT_FOUND.name());
	}

	@Test
	void updatingProductWithUnknownCategoryIsRejected() {
		User staff = staff();
		Category category = categories.create();
		Product product = products.create(category);

		MvcTestResult result = send(staff, mvc.put().uri(PRODUCT_PATH, product.getId()),
				PRODUCT_REQUEST_WITHOUT_DESCRIPTION.formatted(marker() + " Novo", "10.00", UUID.randomUUID()));

		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNKNOWN_CATEGORY.name());
		assertThat(categoryIdOf(product.getId())).isEqualTo(category.getId());
	}

	@Test
	void staffDisablesAndEnablesProduct() throws UnsupportedEncodingException {
		User staff = staff();
		Product product = products.create(categories.create());

		MvcTestResult disabled = post(DISABLE_PATH, staff, product.getId());
		MvcTestResult disabledAgain = post(DISABLE_PATH, staff, product.getId());
		MvcTestResult enabled = post(ENABLE_PATH, staff, product.getId());

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
		assertThat(productRepository.findById(product.getId())).map(Product::isActive).contains(true);
	}

	@Test
	void enablingUnknownProductIsRejected() {
		User staff = staff();

		MvcTestResult result = post(ENABLE_PATH, staff, UUID.randomUUID());

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.PRODUCT_NOT_FOUND.name());
	}

	@Test
	void staffDeletesProduct() {
		User staff = staff();
		Product product = products.create(categories.create());

		MvcTestResult result = delete(staff, product.getId());

		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(productRepository.existsById(product.getId())).isFalse();
	}

	@Test
	void deletingProductThatAppearsInOrdersIsRejected() {
		User staff = staff();
		Product ordered = products.create(categories.create());
		orders.create(users.create(), ordered);

		MvcTestResult result = delete(staff, ordered.getId());

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.PRODUCT_IN_USE.name());
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo(Messages.PRODUCT_IN_USE.text());
		assertThat(productRepository.existsById(ordered.getId())).isTrue();
	}

	@Test
	void deletingUnusedProductStillWorks() {
		User staff = staff();
		Category category = categories.create();
		Product ordered = products.create(category);
		Product unused = products.create(category);
		orders.create(users.create(), ordered);

		MvcTestResult result = delete(staff, unused.getId());

		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(productRepository.existsById(unused.getId())).isFalse();
		assertThat(productRepository.existsById(ordered.getId())).isTrue();
	}

	@Test
	void deletingUnknownProductIsRejected() {
		User staff = staff();

		MvcTestResult result = delete(staff, UUID.randomUUID());

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.PRODUCT_NOT_FOUND.name());
	}

	@Test
	void customerCannotManageProducts() {
		User customer = users.create();
		Category category = categories.create();
		Product product = products.create(category);
		String request = PRODUCT_REQUEST_WITHOUT_DESCRIPTION.formatted(marker() + " Novo", "10.00", category.getId());

		List<MvcTestResult> results = List.of(
				mvc.get()
					.uri(CatalogConstants.PRODUCTS_PATH)
					.header(HttpHeaders.AUTHORIZATION, tokens.bearer(customer))
					.exchange(),
				send(customer, mvc.post().uri(CatalogConstants.PRODUCTS_PATH), request),
				send(customer, mvc.put().uri(PRODUCT_PATH, product.getId()), request),
				post(DISABLE_PATH, customer, product.getId()), post(ENABLE_PATH, customer, product.getId()),
				delete(customer, product.getId()));

		assertThat(results).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
		});
		assertThat(productRepository.findById(product.getId())).map(Product::isActive).contains(true);
	}

	private List<Product> productNamed(String name) {
		return productRepository.findAll().stream().filter(product -> product.getName().equals(name)).toList();
	}

	private UUID categoryIdOf(UUID productId) {
		return transactions
			.execute(status -> productRepository.findById(productId).orElseThrow().getCategory().getId());
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
		return mvc.delete().uri(PRODUCT_PATH, id).header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller)).exchange();
	}

	private User staff() {
		return users.create(user -> user.changeRole(Role.STAFF));
	}

	private String marker() {
		return "prd" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

}
