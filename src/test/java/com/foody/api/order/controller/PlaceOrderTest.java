package com.foody.api.order.controller;

import static com.foody.testsupport.order.OrderRequests.address;
import static com.foody.testsupport.order.OrderRequests.item;
import static com.foody.testsupport.order.OrderRequests.order;
import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.seed.catalog.CategoryFactory;
import com.foody.seed.catalog.ProductFactory;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.order.OrderRequests;
import com.foody.testsupport.security.AccessTokens;
import com.foody.testsupport.time.MutableClock;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class PlaceOrderTest {

	private static final long FIRST_ORDER_NUMBER = 1001;

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

	@Autowired
	private MutableClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void customerPlacesOrderPricedByTheServer() {
		User customer = users.create();
		Category category = categories.create();
		Product smash = products.create(category, "Smash duplo", new BigDecimal("34.90"));
		Product lemonade = products.create(category, "Limonada da casa", new BigDecimal("12.00"));
		clock.fixAt(Instant.parse("2026-10-17T22:02:00.123456Z"));
		String deliveryAddress = address(" 01310100 ", " Avenida Paulista ", " 1000 ", "   ", " Bela Vista ",
				" São Paulo ", "SP");

		MvcTestResult result = place(customer,
				order(deliveryAddress, item(smash.getId(), 2), item(lemonade.getId(), 3)));

		assertThat(result).hasStatus(HttpStatus.CREATED);
		assertThat(result).bodyJson().extractingPath("$.id").asString().isNotBlank();
		assertThat(result).bodyJson()
			.extractingPath("$.number")
			.asNumber()
			.satisfies(number -> assertThat(number.longValue()).isGreaterThanOrEqualTo(FIRST_ORDER_NUMBER));
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.RECEIVED.name());
		assertThat(result).bodyJson().extractingPath("$.live").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.placedAt").isEqualTo("2026-10-17T22:02:00.123456Z");
		assertThat(result).bodyJson()
			.extractingPath("$.items[*].productId")
			.asArray()
			.containsExactly(smash.getId().toString(), lemonade.getId().toString());
		assertThat(result).bodyJson()
			.extractingPath("$.items[*].productName")
			.asArray()
			.containsExactly("Smash duplo", "Limonada da casa");
		assertThat(result).bodyJson().extractingPath("$.items[*].unitPrice").asArray().containsExactly(34.9, 12.0);
		assertThat(result).bodyJson().extractingPath("$.items[*].quantity").asArray().containsExactly(2, 3);
		assertThat(result).bodyJson().extractingPath("$.items[*].subtotal").asArray().containsExactly(69.8, 36.0);
		assertThat(result).bodyJson().extractingPath("$.total").isEqualTo(105.8);
		assertThat(result).bodyJson().extractingPath("$.deliveryAddress.zipCode").isEqualTo("01310-100");
		assertThat(result).bodyJson().extractingPath("$.deliveryAddress.street").isEqualTo("Avenida Paulista");
		assertThat(result).bodyJson().extractingPath("$.deliveryAddress.number").isEqualTo("1000");
		assertThat(result).bodyJson().extractingPath("$.deliveryAddress.complement").isNull();
		assertThat(result).bodyJson().extractingPath("$.deliveryAddress.neighborhood").isEqualTo("Bela Vista");
		assertThat(result).bodyJson().extractingPath("$.deliveryAddress.city").isEqualTo("São Paulo");
		assertThat(result).bodyJson().extractingPath("$.deliveryAddress.state").isEqualTo("SP");
		assertThat(result).bodyJson().extractingPath("$.history").asArray().hasSize(1);
		assertThat(result).bodyJson().extractingPath("$.history[0].fromStatus").isNull();
		assertThat(result).bodyJson().extractingPath("$.history[0].toStatus").isEqualTo(OrderStatus.RECEIVED.name());
		assertThat(result).bodyJson()
			.extractingPath("$.history[0].occurredAt")
			.isEqualTo("2026-10-17T22:02:00.123456Z");
		assertThat(result).bodyJson().extractingPath("$.history[0].actorSide").isEqualTo(ActorSide.CUSTOMER.name());
		assertThat(result).bodyJson().extractingPath("$.history[0].reason").isNull();
		assertThat(result).bodyJson().extractingPath("$.history[0].note").isNull();
		assertThat(result).bodyJson().extractingPath("$.cancellation").isNull();
	}

	@Test
	void orderNumbersIncrease() throws UnsupportedEncodingException {
		User customer = users.create();
		Product product = products.create(categories.create());
		String request = order(OrderRequests.VALID_ADDRESS, item(product.getId(), 1));

		MvcTestResult first = place(customer, request);
		MvcTestResult second = place(customer, request);

		assertThat(first).hasStatus(HttpStatus.CREATED);
		assertThat(second).hasStatus(HttpStatus.CREATED);
		assertThat(numberOf(second)).isGreaterThan(numberOf(first));
	}

	@Test
	void placingOrderWithUnavailableProductIsRejected() {
		User customer = users.create();
		Category open = categories.create();
		Category closed = categories.create(Category::disable);
		Product smash = products.create(open, "Smash duplo", new BigDecimal("34.90"));
		Product bauru = products.create(open, "Bauru da casa", new BigDecimal("30.00"), Product::disable);
		Product margherita = products.create(closed, "Margherita da casa", new BigDecimal("49.90"));
		UUID missing = UUID.randomUUID();

		MvcTestResult result = place(customer, order(OrderRequests.VALID_ADDRESS, item(smash.getId(), 1),
				item(bauru.getId(), 1), item(margherita.getId(), 1), item(missing, 1)));

		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.PRODUCT_UNAVAILABLE.name());
		assertThat(result).bodyJson()
			.extractingPath("$.detail")
			.asString()
			.startsWith(Messages.PRODUCT_UNAVAILABLE.text())
			.contains("Bauru da casa", "Margherita da casa", missing.toString())
			.doesNotContain("Smash duplo");
		assertThat(myOrders(customer)).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(0);
	}

	@ParameterizedTest
	@MethodSource("invalidAddresses")
	void placingOrderRejectsInvalidAddress(String deliveryAddress, List<String> rejectedFields) {
		User customer = users.create();
		Product product = products.create(categories.create());

		MvcTestResult result = place(customer, order(deliveryAddress, item(product.getId(), 1)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		assertThat(result).bodyJson()
			.extractingPath("$.errors[*].field")
			.asArray()
			.containsOnlyElementsOf(rejectedFields)
			.containsAll(rejectedFields);
		assertThat(myOrders(customer)).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(0);
	}

	@Test
	void placingOrderExplainsTheZipCodeFormat() {
		User customer = users.create();
		Product product = products.create(categories.create());
		String deliveryAddress = address("1310-100", "Avenida Paulista", "1000", null, "Bela Vista", "São Paulo", "SP");

		MvcTestResult result = place(customer, order(deliveryAddress, item(product.getId(), 1)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson()
			.extractingPath("$.errors[*].field")
			.asArray()
			.containsExactly("deliveryAddress.zipCode");
		assertThat(result).bodyJson().extractingPath("$.errors[0].message").isEqualTo(Messages.INVALID_ZIP_CODE.text());
	}

	@Test
	void placingOrderRejectsUnknownState() {
		User customer = users.create();
		Product product = products.create(categories.create());
		String deliveryAddress = address("01310-100", "Avenida Paulista", "1000", null, "Bela Vista", "São Paulo",
				"XX");

		MvcTestResult result = place(customer, order(deliveryAddress, item(product.getId(), 1)));

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.MALFORMED_REQUEST.name());
		assertThat(myOrders(customer)).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(0);
	}

	@Test
	void placingOrderRejectsDuplicateProductsAndOutOfRangeQuantities() {
		User customer = users.create();
		Category category = categories.create();
		Product product = products.create(category);
		Product other = products.create(category);

		MvcTestResult duplicated = place(customer,
				order(OrderRequests.VALID_ADDRESS, item(product.getId(), 1), item(product.getId(), 2)));
		MvcTestResult tooFew = place(customer, order(OrderRequests.VALID_ADDRESS, item(product.getId(), 0)));
		MvcTestResult tooMany = place(customer,
				order(OrderRequests.VALID_ADDRESS, item(product.getId(), 1), item(other.getId(), 21)));
		MvcTestResult empty = place(customer, order(OrderRequests.VALID_ADDRESS));

		assertThat(List.of(duplicated, tooFew, tooMany, empty)).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		});
		assertThat(duplicated).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("items");
		assertThat(duplicated).bodyJson()
			.extractingPath("$.errors[0].message")
			.isEqualTo(Messages.DUPLICATE_CART_PRODUCT.text());
		assertThat(tooFew).bodyJson()
			.extractingPath("$.errors[*].field")
			.asArray()
			.containsExactly("items[0].quantity");
		assertThat(tooMany).bodyJson()
			.extractingPath("$.errors[*].field")
			.asArray()
			.containsExactly("items[1].quantity");
		assertThat(empty).bodyJson().extractingPath("$.errors[*].field").asArray().containsExactly("items");
		assertThat(myOrders(customer)).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(0);
	}

	@Test
	void staffCannotPlaceOrders() {
		User staff = users.create(user -> user.changeRole(Role.STAFF));
		Product product = products.create(categories.create());

		MvcTestResult result = place(staff, order(OrderRequests.VALID_ADDRESS, item(product.getId(), 1)));

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
	}

	@Test
	void anonymousCannotPlaceOrders() {
		Product product = products.create(categories.create());

		MvcTestResult result = mvc.post()
			.uri(RouteConstants.MY_ORDERS_PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.content(order(OrderRequests.VALID_ADDRESS, item(product.getId(), 1)))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
	}

	static Stream<Arguments> invalidAddresses() {
		return Stream.of(
				Arguments.of(address("01310-1000", "Avenida Paulista", "1000", null, "Bela Vista", "São Paulo", "SP"),
						List.of("deliveryAddress.zipCode")),
				Arguments.of(address("abcde-fgh", "Avenida Paulista", "1000", null, "Bela Vista", "São Paulo", "SP"),
						List.of("deliveryAddress.zipCode")),
				Arguments.of(address("01310-100", "   ", "", null, " ", "\\t", "SP"),
						List.of("deliveryAddress.street", "deliveryAddress.number", "deliveryAddress.neighborhood",
								"deliveryAddress.city")),
				Arguments.of(address(null, null, null, null, null, null, null),
						List.of("deliveryAddress.zipCode", "deliveryAddress.street", "deliveryAddress.number",
								"deliveryAddress.neighborhood", "deliveryAddress.city", "deliveryAddress.state")),
				Arguments.of(address("01310-100", "A".repeat(121), "1".repeat(11), "C".repeat(61), "B", "S", "SP"),
						List.of("deliveryAddress.street", "deliveryAddress.number", "deliveryAddress.complement",
								"deliveryAddress.neighborhood", "deliveryAddress.city")),
				Arguments.of("null", List.of("deliveryAddress")));
	}

	private MvcTestResult place(User caller, String body) {
		return mvc.post()
			.uri(RouteConstants.MY_ORDERS_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	private MvcTestResult myOrders(User caller) {
		return mvc.get()
			.uri(RouteConstants.MY_ORDERS_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.exchange();
	}

	private long numberOf(MvcTestResult result) throws UnsupportedEncodingException {
		Number number = JsonPath.read(result.getResponse().getContentAsString(), "$.number");
		return number.longValue();
	}

}
