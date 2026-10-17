package com.foody.api.order.controller;

import static com.foody.testsupport.order.OrderRequests.item;
import static com.foody.testsupport.order.OrderRequests.order;
import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.repository.ProductRepository;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.seed.catalog.CategoryFactory;
import com.foody.seed.catalog.ProductFactory;
import com.foody.seed.order.OrderFactory;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.order.OrderRequests;
import com.foody.testsupport.security.AccessTokens;
import com.foody.testsupport.time.MutableClock;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionOperations;

@ApiIntegrationTest
class MyOrdersTest {

	private static final String MY_ORDER_PATH = RouteConstants.MY_ORDERS_PATH + RouteConstants.ID_PATH;

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

	@Autowired
	private MutableClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void customerListsOnlyTheirOrdersNewestFirst() throws UnsupportedEncodingException {
		User customer = users.create();
		User someoneElse = users.create();
		Category category = categories.create();
		Product smash = products.create(category, "Smash duplo", new BigDecimal("34.90"));
		Product lemonade = products.create(category, "Limonada da casa", new BigDecimal("12.00"));
		String oldest = placeAt(customer, "2026-10-10T16:05:00Z", item(smash.getId(), 1));
		String newest = placeAt(customer, "2026-10-17T22:02:00Z", item(smash.getId(), 1), item(lemonade.getId(), 2));
		String middle = placeAt(customer, "2026-10-16T23:14:00Z", item(lemonade.getId(), 3));
		placeAt(someoneElse, "2026-10-17T23:00:00Z", item(smash.getId(), 1));

		MvcTestResult result = myOrders(customer);

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(newest, middle, oldest);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].placedAt")
			.asArray()
			.containsExactly("2026-10-17T22:02:00Z", "2026-10-16T23:14:00Z", "2026-10-10T16:05:00Z");
		assertThat(result).bodyJson().extractingPath("$.data[*].total").asArray().containsExactly(58.9, 36.0, 34.9);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].status")
			.asArray()
			.containsOnly(OrderStatus.RECEIVED.name());
		assertThat(result).bodyJson().extractingPath("$.data[*].live").asArray().containsOnly(true);
		assertThat(result).bodyJson().extractingPath("$.data[0].number").asNumber().isNotNull();
		assertThat(result).bodyJson()
			.extractingPath("$.data[0].items[*].productName")
			.asArray()
			.containsExactly("Smash duplo", "Limonada da casa");
		assertThat(result).bodyJson().extractingPath("$.data[0].items[*].quantity").asArray().containsExactly(1, 2);
		assertThat(result).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(3);
	}

	@Test
	void customerListSortsByTotal() throws UnsupportedEncodingException {
		User customer = users.create();
		Category category = categories.create();
		Product cheap = products.create(category, "Água", new BigDecimal("5.00"));
		Product expensive = products.create(category, "Picanha", new BigDecimal("89.90"));
		String cheapOrder = placeAt(customer, "2026-10-17T20:00:00Z", item(cheap.getId(), 1));
		String expensiveOrder = placeAt(customer, "2026-10-17T19:00:00Z", item(expensive.getId(), 1));

		MvcTestResult result = mvc.get()
			.uri(RouteConstants.MY_ORDERS_PATH)
			.param("sort", "total,desc")
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(customer))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].id")
			.asArray()
			.containsExactly(expensiveOrder, cheapOrder);
	}

	@Test
	void customerListRejectsUnknownSortProperty() {
		User customer = users.create();

		MvcTestResult result = mvc.get()
			.uri(RouteConstants.MY_ORDERS_PATH)
			.param("sort", "status,asc")
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(customer))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_SORT.name());
	}

	@Test
	void customerSeesTheirOrderDetail() throws UnsupportedEncodingException {
		User customer = users.create();
		Category category = categories.create();
		Product smash = products.create(category, "Smash duplo", new BigDecimal("34.90"));
		String id = placeAt(customer, "2026-10-17T22:02:00Z", item(smash.getId(), 2));
		transactions.executeWithoutResult(status -> productRepository.findById(smash.getId())
			.orElseThrow()
			.update(category, "Smash triplo", null, new BigDecimal("44.90")));

		MvcTestResult result = detail(customer, UUID.fromString(id));

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(id);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.RECEIVED.name());
		assertThat(result).bodyJson().extractingPath("$.live").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.placedAt").isEqualTo("2026-10-17T22:02:00Z");
		assertThat(result).bodyJson()
			.extractingPath("$.items[*].productId")
			.asArray()
			.containsExactly(smash.getId().toString());
		assertThat(result).bodyJson().extractingPath("$.items[0].productName").isEqualTo("Smash duplo");
		assertThat(result).bodyJson().extractingPath("$.items[0].unitPrice").isEqualTo(34.9);
		assertThat(result).bodyJson().extractingPath("$.items[0].quantity").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.items[0].subtotal").isEqualTo(69.8);
		assertThat(result).bodyJson().extractingPath("$.total").isEqualTo(69.8);
		assertThat(result).bodyJson().extractingPath("$.deliveryAddress.zipCode").isEqualTo("01310-100");
		assertThat(result).bodyJson().extractingPath("$.deliveryAddress.complement").isEqualTo("Apto 42");
		assertThat(result).bodyJson().extractingPath("$.history[*].toStatus").asArray().containsExactly("RECEIVED");
		assertThat(result).bodyJson().extractingPath("$.history[0].actorSide").isEqualTo(ActorSide.CUSTOMER.name());
		assertThat(result).bodyJson().extractingPath("$.cancellation").isNull();
	}

	@Test
	void customerDetailHasNoStaffNames() throws UnsupportedEncodingException {
		User customer = users.create();
		User staff = users.create("Carlos Equipe Reservado", "carlos.reservado." + UUID.randomUUID() + "@foody.test",
				user -> user.changeRole(Role.STAFF));
		Instant placedAt = Instant.parse("2026-10-17T22:02:00Z");
		Order order = orders.create(customer, placedAt, placed -> placed.advance(OrderStatus.PREPARING, staff,
				ActorSide.RESTAURANT, placedAt.plus(Duration.ofMinutes(3))));

		MvcTestResult result = detail(customer, order.getId());

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.history[*].actorSide")
			.asArray()
			.containsExactly(ActorSide.CUSTOMER.name(), ActorSide.RESTAURANT.name());
		assertThat(result).bodyJson().doesNotHavePath("$.history[0].actorName");
		assertThat(result).bodyJson().doesNotHavePath("$.history[1].actorName");
		assertThat(result).bodyJson().doesNotHavePath("$.customer");
		assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain(staff.getName(),
				staff.getEmail());
	}

	@Test
	void customerCannotSeeAnotherCustomersOrder() throws UnsupportedEncodingException {
		User owner = users.create();
		User intruder = users.create();
		Product product = products.create(categories.create());
		String id = placeAt(owner, "2026-10-17T22:02:00Z", item(product.getId(), 1));

		MvcTestResult othersOrder = detail(intruder, UUID.fromString(id));
		MvcTestResult unknownOrder = detail(intruder, UUID.randomUUID());

		assertThat(List.of(othersOrder, unknownOrder)).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.ORDER_NOT_FOUND.name());
		});
		assertThat(myOrders(intruder)).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(0);
	}

	@Test
	void staffCannotUseCustomerOrderEndpoints() throws UnsupportedEncodingException {
		User customer = users.create();
		User staff = users.create(user -> user.changeRole(Role.STAFF));
		Product product = products.create(categories.create());
		String id = placeAt(customer, "2026-10-17T22:02:00Z", item(product.getId(), 1));

		MvcTestResult list = myOrders(staff);
		MvcTestResult detail = detail(staff, UUID.fromString(id));

		assertThat(List.of(list, detail)).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
		});
	}

	private String placeAt(User customer, String placedAt, String... items) throws UnsupportedEncodingException {
		clock.fixAt(Instant.parse(placedAt));
		MvcTestResult result = mvc.post()
			.uri(RouteConstants.MY_ORDERS_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(customer))
			.contentType(MediaType.APPLICATION_JSON)
			.content(order(OrderRequests.VALID_ADDRESS, items))
			.exchange();
		clock.reset();
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private MvcTestResult myOrders(User caller) {
		return mvc.get()
			.uri(RouteConstants.MY_ORDERS_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.exchange();
	}

	private MvcTestResult detail(User caller, UUID id) {
		return mvc.get().uri(MY_ORDER_PATH, id).header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller)).exchange();
	}

}
