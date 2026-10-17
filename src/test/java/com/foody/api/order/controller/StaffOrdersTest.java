package com.foody.api.order.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.seed.order.OrderFactory;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import com.foody.testsupport.time.MutableClock;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class StaffOrdersTest {

	private static final String ORDER_PATH = OrderConstants.ORDERS_PATH + RouteConstants.ID_PATH;

	private static final String EMAIL_DOMAIN = "@foody.test";

	private static final Duration MINUTE = Duration.ofMinutes(1);

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserFactory users;

	@Autowired
	private OrderFactory orders;

	@Autowired
	private AccessTokens tokens;

	@Autowired
	private MutableClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void staffListsOrdersFilteredByStatuses() {
		User staff = staff();
		String marker = marker();
		User customer = customer(marker + " Ana Paula");
		Instant placedAt = Instant.parse("2026-09-01T15:00:00Z");
		Order received = orders.create(customer, placedAt);
		Order preparing = orders.create(customer, placedAt.plus(MINUTE), advancedTo(staff, OrderStatus.PREPARING));
		Order cancelled = orders.create(customer, placedAt.plus(MINUTE.multipliedBy(2)), cancelledBy(customer));
		Order delivered = orders.create(customer, placedAt.plus(MINUTE.multipliedBy(3)),
				advancedTo(staff, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED));

		MvcTestResult result = list(staff, "search", marker, "status", OrderStatus.PREPARING.name(), "status",
				OrderStatus.CANCELLED.name());

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].id")
			.asArray()
			.containsExactly(idOf(cancelled), idOf(preparing))
			.doesNotContain(idOf(received), idOf(delivered));
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].status")
			.asArray()
			.containsExactly(OrderStatus.CANCELLED.name(), OrderStatus.PREPARING.name());
		assertThat(result).bodyJson().extractingPath("$.data[*].live").asArray().containsExactly(false, true);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].customerName")
			.asArray()
			.containsOnly(customer.getName());
		assertThat(result).bodyJson().extractingPath("$.data[1].number").isEqualTo((int) preparing.getNumber());
		assertThat(result).bodyJson()
			.extractingPath("$.data[1].total")
			.asNumber()
			.satisfies(total -> assertThat(total.doubleValue()).isEqualTo(preparing.getTotal().doubleValue()));
		assertThat(result).bodyJson()
			.extractingPath("$.data[1].items[0].productName")
			.isEqualTo(preparing.getItems().getFirst().getProductName());
		assertThat(result).bodyJson()
			.extractingPath("$.data[1].items[0].quantity")
			.isEqualTo(preparing.getItems().getFirst().getQuantity());
		assertThat(result).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(2);
	}

	@Test
	void staffBoardQueryReturnsActiveOrdersOldestFirst() throws UnsupportedEncodingException {
		User staff = staff();
		User customer = customer(marker() + " Bruna Quadro");
		Instant opening = Instant.parse("1990-01-01T12:00:00Z");
		Order outForDelivery = orders.create(customer, opening.plus(MINUTE.multipliedBy(20)),
				advancedTo(staff, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY));
		Order received = orders.create(customer, opening);
		Order delivered = orders.create(customer, opening.plus(MINUTE.multipliedBy(5)),
				advancedTo(staff, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED));
		Order preparing = orders.create(customer, opening.plus(MINUTE.multipliedBy(10)),
				advancedTo(staff, OrderStatus.PREPARING));
		Order cancelled = orders.create(customer, opening.plus(MINUTE.multipliedBy(15)), cancelledBy(customer));

		MvcTestResult result = list(staff, "status", OrderStatus.RECEIVED.name(), "status",
				OrderStatus.PREPARING.name(), "status", OrderStatus.OUT_FOR_DELIVERY.name(), "pageSize", "100", "sort",
				"placedAt,asc");

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.meta.pageSize").isEqualTo(100);
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].id")
			.asArray()
			.startsWith(idOf(received), idOf(preparing), idOf(outForDelivery))
			.doesNotContain(idOf(delivered), idOf(cancelled));
		assertThat(result).bodyJson()
			.extractingPath("$.data[*].status")
			.asArray()
			.allMatch(status -> OrderStatus.valueOf(status.toString()).isActive());
		assertThat(result).bodyJson().extractingPath("$.data[*].live").asArray().containsOnly(true);
		assertThat(placedAtOf(result)).isSorted();
	}

	@Test
	void staffFiltersOrdersByDayInRestaurantTimeZone() {
		User staff = staff();
		String marker = marker();
		User customer = customer(marker + " Carla Fuso");
		Order lateOnTheNinth = placedWithClockAt(customer, "2001-03-10T02:30:00Z");
		Order earlyOnTheTenth = placedWithClockAt(customer, "2001-03-10T03:30:00Z");
		Order onTheEleventh = placedWithClockAt(customer, "2001-03-11T14:00:00Z");

		MvcTestResult ninth = list(staff, "search", marker, "from", "2001-03-09", "to", "2001-03-09");
		MvcTestResult tenth = list(staff, "search", marker, "from", "2001-03-10", "to", "2001-03-10");
		MvcTestResult fromTheTenth = list(staff, "search", marker, "from", "2001-03-10");
		MvcTestResult untilTheTenth = list(staff, "search", marker, "to", "2001-03-10");

		assertThat(ninth).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(lateOnTheNinth));
		assertThat(tenth).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(earlyOnTheTenth));
		assertThat(fromTheTenth).bodyJson()
			.extractingPath("$.data[*].id")
			.asArray()
			.containsExactly(idOf(onTheEleventh), idOf(earlyOnTheTenth));
		assertThat(untilTheTenth).bodyJson()
			.extractingPath("$.data[*].id")
			.asArray()
			.containsExactly(idOf(earlyOnTheTenth), idOf(lateOnTheNinth));
	}

	@Test
	void staffListRejectsInvalidDate() {
		User staff = staff();

		MvcTestResult result = list(staff, "from", "2001-02-30");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_PARAMETER.name());
	}

	@Test
	void staffSearchesByNumberOrCustomerName() {
		User staff = staff();
		String marker = marker();
		User ana = customer(marker + " Ana Souza");
		User bruno = customer(marker + " Bruno Lima");
		Order anasOrder = orders.create(ana, Instant.parse("2026-09-02T15:00:00Z"));
		Order brunosOrder = orders.create(bruno, Instant.parse("2026-09-02T16:00:00Z"));

		MvcTestResult byHashedNumber = list(staff, "search", "#" + brunosOrder.getNumber());
		MvcTestResult byNumber = list(staff, "search", " " + brunosOrder.getNumber() + " ");
		MvcTestResult byName = list(staff, "search", (marker + " ana").toUpperCase(Locale.ROOT));
		MvcTestResult byWildcard = list(staff, "search", marker + "%");

		assertThat(byHashedNumber).bodyJson()
			.extractingPath("$.data[*].id")
			.asArray()
			.containsExactly(idOf(brunosOrder));
		assertThat(byNumber).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(brunosOrder));
		assertThat(byName).bodyJson().extractingPath("$.data[*].id").asArray().containsExactly(idOf(anasOrder));
		assertThat(byWildcard).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(0);
	}

	@Test
	void staffListRejectsUnknownSortProperty() {
		User staff = staff();

		MvcTestResult result = list(staff, "sort", "customerName,asc");

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_SORT.name());
	}

	@Test
	void staffSeesOrderDetailWithCustomerAndActors() {
		String marker = marker();
		User staff = users.create(marker + " Carlos Equipe", marker + ".carlos" + EMAIL_DOMAIN,
				user -> user.changeRole(Role.STAFF));
		User viewer = staff();
		User customer = customer(marker + " Ana Cliente");
		Instant placedAt = Instant.parse("2026-09-03T22:02:00Z");
		Order order = orders.create(customer, placedAt, placed -> {
			placed.advance(OrderStatus.PREPARING, staff, ActorSide.RESTAURANT, placedAt.plus(MINUTE.multipliedBy(3)));
			placed.cancel(staff, ActorSide.RESTAURANT, CancellationReason.ITEM_UNAVAILABLE, "Acabou o pão",
					placedAt.plus(MINUTE.multipliedBy(8)));
		});

		MvcTestResult result = detail(viewer, order.getId());

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(idOf(order));
		assertThat(result).bodyJson().extractingPath("$.number").isEqualTo((int) order.getNumber());
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.CANCELLED.name());
		assertThat(result).bodyJson().extractingPath("$.live").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.placedAt").isEqualTo("2026-09-03T22:02:00Z");
		assertThat(result).bodyJson().extractingPath("$.customer.id").isEqualTo(customer.getId().toString());
		assertThat(result).bodyJson().extractingPath("$.customer.name").isEqualTo(customer.getName());
		assertThat(result).bodyJson().extractingPath("$.customer.email").isEqualTo(customer.getEmail());
		assertThat(result).bodyJson()
			.extractingPath("$.items[0].productName")
			.isEqualTo(order.getItems().getFirst().getProductName());
		assertThat(result).bodyJson().extractingPath("$.deliveryAddress.state").isEqualTo("SP");
		assertThat(result).bodyJson()
			.extractingPath("$.history[*].fromStatus")
			.asArray()
			.containsExactly(null, OrderStatus.RECEIVED.name(), OrderStatus.PREPARING.name());
		assertThat(result).bodyJson()
			.extractingPath("$.history[*].toStatus")
			.asArray()
			.containsExactly(OrderStatus.RECEIVED.name(), OrderStatus.PREPARING.name(), OrderStatus.CANCELLED.name());
		assertThat(result).bodyJson()
			.extractingPath("$.history[*].actorName")
			.asArray()
			.containsExactly(customer.getName(), staff.getName(), staff.getName());
		assertThat(result).bodyJson()
			.extractingPath("$.history[*].actorSide")
			.asArray()
			.containsExactly(ActorSide.CUSTOMER.name(), ActorSide.RESTAURANT.name(), ActorSide.RESTAURANT.name());
		assertThat(result).bodyJson()
			.extractingPath("$.history[*].occurredAt")
			.asArray()
			.containsExactly("2026-09-03T22:02:00Z", "2026-09-03T22:05:00Z", "2026-09-03T22:10:00Z");
		assertThat(result).bodyJson()
			.extractingPath("$.history[2].reason")
			.isEqualTo(CancellationReason.ITEM_UNAVAILABLE.name());
		assertThat(result).bodyJson().extractingPath("$.history[2].note").isEqualTo("Acabou o pão");
		assertThat(result).bodyJson()
			.extractingPath("$.cancellation.cancelledBy")
			.isEqualTo(ActorSide.RESTAURANT.name());
		assertThat(result).bodyJson()
			.extractingPath("$.cancellation.reason")
			.isEqualTo(CancellationReason.ITEM_UNAVAILABLE.name());
		assertThat(result).bodyJson().extractingPath("$.cancellation.note").isEqualTo("Acabou o pão");
	}

	@Test
	void staffDetailOfUnknownOrderIsNotFound() {
		User staff = staff();

		MvcTestResult result = detail(staff, UUID.randomUUID());

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.ORDER_NOT_FOUND.name());
	}

	@Test
	void customerCannotUseStaffOrderEndpoints() {
		User customer = users.create();
		Order order = orders.create(customer);

		MvcTestResult list = list(customer);
		MvcTestResult detail = detail(customer, order.getId());

		assertThat(List.of(list, detail)).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
		});
	}

	private Order placedWithClockAt(User customer, String instant) {
		clock.fixAt(Instant.parse(instant));
		Order order = orders.create(customer);
		clock.reset();
		return order;
	}

	private Consumer<Order> advancedTo(User staff, OrderStatus... statuses) {
		return order -> {
			Instant occurredAt = order.getPlacedAt();
			for (OrderStatus status : statuses) {
				occurredAt = occurredAt.plus(MINUTE);
				order.advance(status, staff, ActorSide.RESTAURANT, occurredAt);
			}
		};
	}

	private Consumer<Order> cancelledBy(User customer) {
		return order -> order.cancel(customer, ActorSide.CUSTOMER, CancellationReason.CHANGED_MIND, null,
				order.getPlacedAt().plus(MINUTE));
	}

	private List<Instant> placedAtOf(MvcTestResult result) throws UnsupportedEncodingException {
		List<String> placedAt = JsonPath.read(result.getResponse().getContentAsString(), "$.data[*].placedAt");
		return placedAt.stream().map(Instant::parse).toList();
	}

	private MvcTestResult list(User caller, String... parameters) {
		MockMvcTester.MockMvcRequestBuilder request = mvc.get()
			.uri(OrderConstants.ORDERS_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller));
		for (int index = 0; index < parameters.length; index += 2) {
			request.param(parameters[index], parameters[index + 1]);
		}
		return request.exchange();
	}

	private MvcTestResult detail(User caller, UUID id) {
		return mvc.get().uri(ORDER_PATH, id).header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller)).exchange();
	}

	private User staff() {
		return users.create(user -> user.changeRole(Role.STAFF));
	}

	private User customer(String name) {
		return users.create(name, name.toLowerCase(Locale.ROOT).replace(" ", ".") + EMAIL_DOMAIN);
	}

	private String idOf(Order order) {
		return order.getId().toString();
	}

	private String marker() {
		return "ord" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

}
