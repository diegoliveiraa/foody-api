package com.foody.api.order.controller;

import static com.foody.testsupport.order.OrderRequests.cancellation;
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
import com.foody.seed.order.CancellationRequestFactory;
import com.foody.seed.order.OrderFactory;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import com.foody.testsupport.time.MutableClock;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class CustomerOrderLifecycleTest {

	private static final String MY_ORDER_PATH = RouteConstants.MY_ORDERS_PATH + RouteConstants.ID_PATH;

	private static final String MY_CANCEL_PATH = RouteConstants.MY_ORDERS_PATH + RouteConstants.CANCEL_PATH;

	private static final Instant PLACED_AT = Instant.parse("2026-01-15T22:02:00Z");

	private static final Duration MINUTE = Duration.ofMinutes(1);

	private static final List<String> CUSTOMER_REASONS = List.of(CancellationReason.CHANGED_MIND.name(),
			CancellationReason.ORDERED_BY_MISTAKE.name(), CancellationReason.TAKING_TOO_LONG.name(),
			CancellationReason.OTHER.name());

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserFactory users;

	@Autowired
	private OrderFactory orders;

	@Autowired
	private CancellationRequestFactory cancellationRequests;

	@Autowired
	private AccessTokens tokens;

	@Autowired
	private MutableClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void customerCancelsReceivedOrder() {
		User customer = users.create();
		Order order = orders.create(customer, PLACED_AT);
		Instant cancelledAt = PLACED_AT.plus(MINUTE.multipliedBy(4));
		clock.fixAt(cancelledAt);

		MvcTestResult before = detail(customer, order);
		MvcTestResult cancelled = cancel(customer, order, cancellation(CancellationReason.ORDERED_BY_MISTAKE, null));

		assertThat(before).bodyJson().extractingPath("$.canCancel").isEqualTo(true);
		assertThat(before).bodyJson().extractingPath("$.canRequestCancellation").isEqualTo(false);
		assertThat(before).bodyJson()
			.extractingPath("$.cancellationReasons")
			.asArray()
			.containsExactlyElementsOf(CUSTOMER_REASONS);
		assertThat(before).bodyJson().extractingPath("$.cancellationRequest").isNull();
		assertThat(cancelled).hasStatus(HttpStatus.OK);
		assertThat(cancelled).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.CANCELLED.name());
		assertThat(cancelled).bodyJson().extractingPath("$.live").isEqualTo(false);
		assertThat(cancelled).bodyJson()
			.extractingPath("$.cancellation.cancelledBy")
			.isEqualTo(ActorSide.CUSTOMER.name());
		assertThat(cancelled).bodyJson()
			.extractingPath("$.cancellation.reason")
			.isEqualTo(CancellationReason.ORDERED_BY_MISTAKE.name());
		assertThat(cancelled).bodyJson()
			.extractingPath("$.history[1].toStatus")
			.isEqualTo(OrderStatus.CANCELLED.name());
		assertThat(cancelled).bodyJson().extractingPath("$.history[1].actorSide").isEqualTo(ActorSide.CUSTOMER.name());
		assertThat(cancelled).bodyJson().extractingPath("$.history[1].occurredAt").isEqualTo(cancelledAt.toString());
		assertThat(cancelled).bodyJson().extractingPath("$.canCancel").isEqualTo(false);
		assertThat(cancelled).bodyJson().extractingPath("$.canRequestCancellation").isEqualTo(false);
		assertThat(cancelled).bodyJson().extractingPath("$.cancellationReasons").asArray().isEmpty();
	}

	@Test
	void customerCancelRejectsRestaurantReasonAndUnexplainedOther() {
		User customer = users.create();
		Order order = orders.create(customer, PLACED_AT);

		MvcTestResult restaurantReason = cancel(customer, order,
				cancellation(CancellationReason.ITEM_UNAVAILABLE, null));
		MvcTestResult unexplainedOther = cancel(customer, order, cancellation(CancellationReason.OTHER, " "));
		MvcTestResult detail = detail(customer, order);

		assertRefused(restaurantReason, Messages.INVALID_CANCELLATION_REASON);
		assertThat(unexplainedOther).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(unexplainedOther).bodyJson()
			.extractingPath("$.errors[*].field")
			.asArray()
			.containsExactly(OrderConstants.NOTE_ATTRIBUTE);
		assertThat(detail).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.RECEIVED.name());
	}

	@Test
	void customerCannotCancelPreparingOrderDirectly() {
		User customer = users.create();
		Order order = orders.create(customer, PLACED_AT, preparedBy(staff()));

		MvcTestResult result = cancel(customer, order, cancellation(CancellationReason.CHANGED_MIND, null));
		MvcTestResult detail = detail(customer, order);

		assertRefused(result, Messages.CANCELLATION_NOT_ALLOWED);
		assertThat(detail).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.PREPARING.name());
		assertThat(detail).bodyJson().extractingPath("$.canCancel").isEqualTo(false);
		assertThat(detail).bodyJson().extractingPath("$.canRequestCancellation").isEqualTo(true);
		assertThat(detail).bodyJson()
			.extractingPath("$.cancellationReasons")
			.asArray()
			.containsExactlyElementsOf(CUSTOMER_REASONS);
	}

	@Test
	void customerRequestsCancellationWhilePreparing() {
		User customer = users.create();
		Order order = orders.create(customer, PLACED_AT, preparedBy(staff()));
		Instant requestedAt = PLACED_AT.plus(MINUTE.multipliedBy(12));
		clock.fixAt(requestedAt);

		MvcTestResult result = requestCancellation(customer, order,
				cancellation(CancellationReason.TAKING_TOO_LONG, null));
		MvcTestResult list = myOrders(customer);

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.PREPARING.name());
		assertThat(result).bodyJson().extractingPath("$.live").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.cancellationRequest.status").isEqualTo("PENDING");
		assertThat(result).bodyJson()
			.extractingPath("$.cancellationRequest.reason")
			.isEqualTo(CancellationReason.TAKING_TOO_LONG.name());
		assertThat(result).bodyJson()
			.extractingPath("$.cancellationRequest.requestedAt")
			.isEqualTo(requestedAt.toString());
		assertThat(result).bodyJson().extractingPath("$.cancellationRequest.note").isNull();
		assertThat(result).bodyJson().extractingPath("$.cancellationRequest.decidedAt").isNull();
		assertThat(result).bodyJson().extractingPath("$.cancellationRequest.decisionNote").isNull();
		assertThat(result).bodyJson().doesNotHavePath("$.cancellationRequest.decidedByName");
		assertThat(result).bodyJson().extractingPath("$.canCancel").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.canRequestCancellation").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.cancellationReasons").asArray().isEmpty();
		assertThat(result).bodyJson().extractingPath("$.history").asArray().hasSize(2);
		assertThat(list).bodyJson()
			.extractingPath("$.data[?(@.id == '%s')].cancellationRequestStatus".formatted(order.getId()))
			.asArray()
			.containsExactly("PENDING");
	}

	@Test
	void customerCannotRequestCancellationOutsidePreparing() {
		User customer = users.create();
		User staff = staff();
		Order received = orders.create(customer, PLACED_AT);
		Order outForDelivery = orders.create(customer, PLACED_AT,
				preparedBy(staff).andThen(order -> order.advance(OrderStatus.OUT_FOR_DELIVERY, staff,
						ActorSide.RESTAURANT, PLACED_AT.plus(MINUTE.multipliedBy(20)))));

		MvcTestResult tooEarly = requestCancellation(customer, received,
				cancellation(CancellationReason.CHANGED_MIND, null));
		MvcTestResult tooLate = requestCancellation(customer, outForDelivery,
				cancellation(CancellationReason.TAKING_TOO_LONG, null));

		assertRefused(tooEarly, Messages.CANCELLATION_NOT_ALLOWED);
		assertRefused(tooLate, Messages.CANCELLATION_NOT_ALLOWED);
	}

	@Test
	void customerCannotRequestTwice() {
		User customer = users.create();
		User staff = staff();
		Order pending = orders.create(customer, PLACED_AT, preparedBy(staff));
		Order declined = orders.create(customer, PLACED_AT, preparedBy(staff));
		cancellationRequests.create(declined,
				request -> request.decline(staff, null, request.getRequestedAt().plus(MINUTE)));

		MvcTestResult first = requestCancellation(customer, pending,
				cancellation(CancellationReason.CHANGED_MIND, null));
		MvcTestResult second = requestCancellation(customer, pending,
				cancellation(CancellationReason.OTHER, "Vou jantar fora"));
		MvcTestResult afterDecline = requestCancellation(customer, declined,
				cancellation(CancellationReason.TAKING_TOO_LONG, null));

		assertThat(first).hasStatus(HttpStatus.OK);
		assertRefused(second, Messages.CANCELLATION_ALREADY_REQUESTED);
		assertRefused(afterDecline, Messages.CANCELLATION_ALREADY_REQUESTED);
	}

	@Test
	void customerCannotActOnAnotherCustomersOrder() {
		User owner = users.create();
		User intruder = users.create();
		Order received = orders.create(owner, PLACED_AT);
		Order preparing = orders.create(owner, PLACED_AT, preparedBy(staff()));

		MvcTestResult cancel = cancel(intruder, received, cancellation(CancellationReason.CHANGED_MIND, null));
		MvcTestResult request = requestCancellation(intruder, preparing,
				cancellation(CancellationReason.CHANGED_MIND, null));
		MvcTestResult unknown = cancel(intruder, UUID.randomUUID(),
				cancellation(CancellationReason.CHANGED_MIND, null));
		MvcTestResult receivedAfter = detail(owner, received);
		MvcTestResult preparingAfter = detail(owner, preparing);

		assertThat(List.of(cancel, request, unknown)).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.ORDER_NOT_FOUND.name());
		});
		assertThat(receivedAfter).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.RECEIVED.name());
		assertThat(preparingAfter).bodyJson().extractingPath("$.cancellationRequest").isNull();
	}

	@Test
	void customerDetailShowsRequestWithoutStaffNames() throws UnsupportedEncodingException {
		User customer = users.create();
		User staff = users.create("Carlos Reservado " + UUID.randomUUID(),
				"reservado." + UUID.randomUUID() + "@foody.test", user -> user.changeRole(Role.STAFF));
		Order order = orders.create(customer, PLACED_AT, preparedBy(staff));
		Instant decidedAt = PLACED_AT.plus(MINUTE.multipliedBy(15));
		cancellationRequests.create(order,
				request -> request.decline(staff, "O pedido já está quase pronto", decidedAt));

		MvcTestResult result = detail(customer, order);

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.cancellationRequest.status").isEqualTo("DECLINED");
		assertThat(result).bodyJson()
			.extractingPath("$.cancellationRequest.decisionNote")
			.isEqualTo("O pedido já está quase pronto");
		assertThat(result).bodyJson().extractingPath("$.cancellationRequest.decidedAt").isEqualTo(decidedAt.toString());
		assertThat(result).bodyJson().doesNotHavePath("$.cancellationRequest.decidedByName");
		assertThat(result).bodyJson().extractingPath("$.canRequestCancellation").isEqualTo(false);
		assertThat(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).doesNotContain(staff.getName(),
				staff.getEmail());
	}

	@Test
	void staffCannotUseCustomerCommands() {
		User customer = users.create();
		User staff = staff();
		Order received = orders.create(customer, PLACED_AT);
		Order preparing = orders.create(customer, PLACED_AT, preparedBy(staff));

		MvcTestResult cancel = cancel(staff, received, cancellation(CancellationReason.CHANGED_MIND, null));
		MvcTestResult request = requestCancellation(staff, preparing,
				cancellation(CancellationReason.CHANGED_MIND, null));

		assertThat(List.of(cancel, request)).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
		});
	}

	private Consumer<Order> preparedBy(User staff) {
		return order -> order.advance(OrderStatus.PREPARING, staff, ActorSide.RESTAURANT,
				order.getPlacedAt().plus(MINUTE.multipliedBy(3)));
	}

	private MvcTestResult cancel(User caller, Order order, String body) {
		return cancel(caller, order.getId(), body);
	}

	private MvcTestResult cancel(User caller, UUID orderId, String body) {
		return send(caller, mvc.post().uri(MY_CANCEL_PATH, orderId), body);
	}

	private MvcTestResult requestCancellation(User caller, Order order, String body) {
		return send(caller, mvc.post().uri(RouteConstants.MY_ORDER_CANCELLATION_REQUEST_PATH, order.getId()), body);
	}

	private MvcTestResult detail(User caller, Order order) {
		return mvc.get()
			.uri(MY_ORDER_PATH, order.getId())
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.exchange();
	}

	private MvcTestResult myOrders(User caller) {
		return mvc.get()
			.uri(RouteConstants.MY_ORDERS_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.exchange();
	}

	private MvcTestResult send(User caller, MockMvcTester.MockMvcRequestBuilder request, String body) {
		return request.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	private void assertRefused(MvcTestResult result, Messages code) {
		assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code.name());
	}

	private User staff() {
		return users.create(user -> user.changeRole(Role.STAFF));
	}

}
