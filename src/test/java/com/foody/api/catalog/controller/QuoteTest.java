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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class QuoteTest {

	private static final String QUOTE_PATH = CatalogConstants.MENU_PATH + CatalogConstants.QUOTE_PATH;

	private static final String QUOTE_REQUEST = """
			{ "items": [%s] }
			""";

	private static final String ITEM = """
			{ "productId": "%s", "quantity": %d }
			""";

	private static final String ITEM_SEPARATOR = ",";

	private static final int TOO_MANY_ITEMS = 51;

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
	void quotePricesItemsFromTheDatabase() {
		User customer = users.create();
		Category category = categories.create();
		Product smash = products.create(category, "Smash duplo", new BigDecimal("34.90"));
		Product lemonade = products.create(category, "Limonada da casa", new BigDecimal("12.00"));

		MvcTestResult result = quote(customer, item(smash.getId(), 2), item(lemonade.getId(), 3));

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.items[*].productId")
			.asArray()
			.containsExactly(smash.getId().toString(), lemonade.getId().toString());
		assertThat(result).bodyJson()
			.extractingPath("$.items[*].name")
			.asArray()
			.containsExactly("Smash duplo", "Limonada da casa");
		assertThat(result).bodyJson().extractingPath("$.items[*].unitPrice").asArray().containsExactly(34.9, 12.0);
		assertThat(result).bodyJson().extractingPath("$.items[*].quantity").asArray().containsExactly(2, 3);
		assertThat(result).bodyJson().extractingPath("$.items[*].subtotal").asArray().containsExactly(69.8, 36.0);
		assertThat(result).bodyJson().extractingPath("$.items[*].available").asArray().containsExactly(true, true);
		assertThat(result).bodyJson().extractingPath("$.total").isEqualTo(105.8);
	}

	@Test
	void quoteMarksMissingOrDisabledProductsUnavailableAndLeavesThemOutOfTheTotal() {
		User customer = users.create();
		Category open = categories.create();
		Category closed = categories.create(Category::disable);
		Product smash = products.create(open, "Smash duplo", new BigDecimal("34.90"));
		Product bauru = products.create(open, "Bauru", new BigDecimal("30.00"), Product::disable);
		Product margherita = products.create(closed, "Margherita", new BigDecimal("49.90"));
		UUID missing = UUID.randomUUID();

		MvcTestResult result = quote(customer, item(smash.getId(), 1), item(bauru.getId(), 2),
				item(margherita.getId(), 1), item(missing, 3));

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.items[*].productId")
			.asArray()
			.containsExactly(smash.getId().toString(), bauru.getId().toString(), margherita.getId().toString(),
					missing.toString());
		assertThat(result).bodyJson()
			.extractingPath("$.items[*].available")
			.asArray()
			.containsExactly(true, false, false, false);
		assertThat(result).bodyJson()
			.extractingPath("$.items[*].name")
			.asArray()
			.containsExactly("Smash duplo", "Bauru", "Margherita", null);
		assertThat(result).bodyJson()
			.extractingPath("$.items[*].unitPrice")
			.asArray()
			.containsExactly(34.9, null, null, null);
		assertThat(result).bodyJson()
			.extractingPath("$.items[*].subtotal")
			.asArray()
			.containsExactly(34.9, null, null, null);
		assertThat(result).bodyJson().extractingPath("$.items[*].quantity").asArray().containsExactly(1, 2, 1, 3);
		assertThat(result).bodyJson().extractingPath("$.total").isEqualTo(34.9);
	}

	@Test
	void quoteRejectsDuplicateProducts() {
		User customer = users.create();
		Product product = products.create(categories.create());

		MvcTestResult result = quote(customer, item(product.getId(), 1), item(product.getId(), 2));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("items");
		assertThat(result).bodyJson()
			.extractingPath("$.errors[0].message")
			.isEqualTo(Messages.DUPLICATE_CART_PRODUCT.text());
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 21 })
	void quoteRejectsQuantityOutOfRange(int quantity) {
		User customer = users.create();
		Product product = products.create(categories.create());

		MvcTestResult result = quote(customer, item(product.getId(), quantity));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		assertThat(result).bodyJson()
			.extractingPath("$.errors[*].field")
			.asArray()
			.containsExactly("items[0].quantity");
	}

	@Test
	void quoteRejectsEmptyCart() {
		User customer = users.create();

		MvcTestResult result = quote(customer);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("items");
	}

	@Test
	void quoteRejectsMoreThanFiftyItems() {
		User customer = users.create();
		String[] items = Stream.generate(UUID::randomUUID)
			.limit(TOO_MANY_ITEMS)
			.map(productId -> item(productId, 1))
			.toArray(String[]::new);

		MvcTestResult result = quote(customer, items);

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("items");
	}

	@Test
	void staffCannotQuote() {
		User staff = users.create(user -> user.changeRole(Role.STAFF));
		Product product = products.create(categories.create());

		MvcTestResult result = quote(staff, item(product.getId(), 1));

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
	}

	private MvcTestResult quote(User caller, String... items) {
		return mvc.post()
			.uri(QUOTE_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.contentType(MediaType.APPLICATION_JSON)
			.content(QUOTE_REQUEST.formatted(Stream.of(items).collect(Collectors.joining(ITEM_SEPARATOR))))
			.exchange();
	}

	private String item(UUID productId, int quantity) {
		return ITEM.formatted(productId, quantity);
	}

}
