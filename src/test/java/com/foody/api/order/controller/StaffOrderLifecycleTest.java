package com.foody.api.order.controller;

import static com.foody.testsupport.order.OrderRequests.cancellation;
import static com.foody.testsupport.order.OrderRequests.decision;
import static com.foody.testsupport.order.OrderRequests.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.CancellationRequest;
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
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class StaffOrderLifecycleTest {

	private static final String ORDER_PATH = OrderConstants.ORDERS_PATH + RouteConstants.ID_PATH;

	private static final String STATUS_PATH = OrderConstants.ORDERS_PATH + OrderConstants.STATUS_PATH;

	private static final String CANCEL_PATH = OrderConstants.ORDERS_PATH + RouteConstants.CANCEL_PATH;

	private static final String APPROVE_PATH = OrderConstants.ORDERS_PATH + OrderConstants.APPROVE_CANCELLATION_PATH;

	private static final String DECLINE_PATH = OrderConstants.ORDERS_PATH + OrderConstants.DECLINE_CANCELLATION_PATH;

	private static final Instant PLACED_AT = Instant.parse("2026-01-15T22:02:00Z");

	private static final Duration MINUTE = Duration.ofMinutes(1);

	private static final String EMAIL_DOMAIN = "@foody.test";

	private static final String STAFF_EMAIL_PREFIX = "staff.";

	private static final String EMPTY_BODY = "{}";

	private static final List<String> KITCHEN_REASONS = List.of(CancellationReason.ITEM_UNAVAILABLE.name(),
			CancellationReason.STORE_CLOSED.name(), CancellationReason.CUSTOMER_REQUEST.name(),
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

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@AfterEach
	void resetClockAndStatistics() {
		clock.reset();
		statistics().setStatisticsEnabled(false);
	}

	@Test
	void staffAdvancesOrderThroughEveryStatus() {
		User customer = users.create();
		User staff = staff("Carlos Avanço");
		Order order = orders.create(customer, PLACED_AT);

		MvcTestResult preparing = updateStatusAt(staff, order, OrderStatus.PREPARING, minutesAfterPlacing(3));
		MvcTestResult outForDelivery = updateStatusAt(staff, order, OrderStatus.OUT_FOR_DELIVERY,
				minutesAfterPlacing(20));
		MvcTestResult delivered = updateStatusAt(staff, order, OrderStatus.DELIVERED, minutesAfterPlacing(45));

		assertThat(List.of(preparing, outForDelivery, delivered))
			.allSatisfy(result -> assertThat(result).hasStatus(HttpStatus.OK));
		assertThat(preparing).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.PREPARING.name());
		assertThat(preparing).bodyJson()
			.extractingPath("$.allowedTransitions")
			.asArray()
			.containsExactly(OrderStatus.OUT_FOR_DELIVERY.name());
		assertThat(preparing).bodyJson().extractingPath("$.canCancel").isEqualTo(true);
		assertThat(preparing).bodyJson()
			.extractingPath("$.cancellationReasons")
			.asArray()
			.containsExactlyElementsOf(KITCHEN_REASONS);
		assertThat(outForDelivery).bodyJson()
			.extractingPath("$.allowedTransitions")
			.asArray()
			.containsExactly(OrderStatus.DELIVERED.name());
		assertThat(outForDelivery).bodyJson()
			.extractingPath("$.cancellationReasons")
			.asArray()
			.containsExactly(CancellationReason.DELIVERY_FAILED.name());
		assertThat(delivered).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.DELIVERED.name());
		assertThat(delivered).bodyJson().extractingPath("$.live").isEqualTo(false);
		assertThat(delivered).bodyJson().extractingPath("$.allowedTransitions").asArray().isEmpty();
		assertThat(delivered).bodyJson().extractingPath("$.canCancel").isEqualTo(false);
		assertThat(delivered).bodyJson().extractingPath("$.cancellationReasons").asArray().isEmpty();
		assertThat(delivered).bodyJson().extractingPath("$.cancellationRequest").isNull();
		assertThat(delivered).bodyJson()
			.extractingPath("$.history[*].fromStatus")
			.asArray()
			.containsExactly(null, OrderStatus.RECEIVED.name(), OrderStatus.PREPARING.name(),
					OrderStatus.OUT_FOR_DELIVERY.name());
		assertThat(delivered).bodyJson()
			.extractingPath("$.history[*].toStatus")
			.asArray()
			.containsExactly(OrderStatus.RECEIVED.name(), OrderStatus.PREPARING.name(),
					OrderStatus.OUT_FOR_DELIVERY.name(), OrderStatus.DELIVERED.name());
		assertThat(delivered).bodyJson()
			.extractingPath("$.history[*].actorName")
			.asArray()
			.containsExactly(customer.getName(), staff.getName(), staff.getName(), staff.getName());
		assertThat(delivered).bodyJson()
			.extractingPath("$.history[*].actorSide")
			.asArray()
			.containsExactly(ActorSide.CUSTOMER.name(), ActorSide.RESTAURANT.name(), ActorSide.RESTAURANT.name(),
					ActorSide.RESTAURANT.name());
		assertThat(delivered).bodyJson()
			.extractingPath("$.history[*].occurredAt")
			.asArray()
			.containsExactly(PLACED_AT.toString(), minutesAfterPlacing(3).toString(),
					minutesAfterPlacing(20).toString(), minutesAfterPlacing(45).toString());
	}

	@Test
	void staffCannotSkipOrRewindStatus() {
		User staff = staff("Diana Pulo");
		Order order = orders.create(users.create(), PLACED_AT, advancedTo(staff, OrderStatus.PREPARING));

		MvcTestResult skip = updateStatus(staff, order, OrderStatus.DELIVERED);
		MvcTestResult rewind = updateStatus(staff, order, OrderStatus.RECEIVED);
		MvcTestResult repeat = updateStatus(staff, order, OrderStatus.PREPARING);
		MvcTestResult cancelThroughStatus = updateStatus(staff, order, OrderStatus.CANCELLED);
		MvcTestResult detail = detail(staff, order);

		assertThat(List.of(skip, rewind, repeat, cancelThroughStatus))
			.allSatisfy(result -> assertRefused(result, Messages.INVALID_STATUS_TRANSITION));
		assertThat(detail).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.PREPARING.name());
		assertThat(detail).bodyJson().extractingPath("$.history").asArray().hasSize(2);
	}

	@Test
	void staleAdvanceToPassedStatusIsRejected() {
		User firstStaff = staff("Elisa Primeira");
		User staleStaff = staff("Fábio Atrasado");
		Order order = orders.create(users.create(), PLACED_AT);

		MvcTestResult first = updateStatus(firstStaff, order, OrderStatus.PREPARING);
		MvcTestResult stale = updateStatus(staleStaff, order, OrderStatus.PREPARING);
		MvcTestResult detail = detail(firstStaff, order);

		assertThat(first).hasStatus(HttpStatus.OK);
		assertRefused(stale, Messages.INVALID_STATUS_TRANSITION);
		assertThat(detail).bodyJson()
			.extractingPath("$.history[*].toStatus")
			.asArray()
			.containsExactly(OrderStatus.RECEIVED.name(), OrderStatus.PREPARING.name());
		assertThat(detail).bodyJson()
			.extractingPath("$.history[*].actorName")
			.asArray()
			.doesNotContain(staleStaff.getName());
	}

	@Test
	void staffCancelsWithReason() {
		User staff = staff("Gabriel Cozinha");
		Order order = orders.create(users.create(), PLACED_AT, advancedTo(staff, OrderStatus.PREPARING));
		clock.fixAt(minutesAfterPlacing(9));

		MvcTestResult result = cancel(staff, order,
				cancellation(CancellationReason.ITEM_UNAVAILABLE, "  Acabou o pão  "));

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.CANCELLED.name());
		assertThat(result).bodyJson().extractingPath("$.live").isEqualTo(false);
		assertThat(result).bodyJson()
			.extractingPath("$.cancellation.cancelledBy")
			.isEqualTo(ActorSide.RESTAURANT.name());
		assertThat(result).bodyJson()
			.extractingPath("$.cancellation.reason")
			.isEqualTo(CancellationReason.ITEM_UNAVAILABLE.name());
		assertThat(result).bodyJson().extractingPath("$.cancellation.note").isEqualTo("Acabou o pão");
		assertThat(result).bodyJson().extractingPath("$.allowedTransitions").asArray().isEmpty();
		assertThat(result).bodyJson().extractingPath("$.canCancel").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.cancellationReasons").asArray().isEmpty();
		assertThat(result).bodyJson().extractingPath("$.history[2].fromStatus").isEqualTo(OrderStatus.PREPARING.name());
		assertThat(result).bodyJson().extractingPath("$.history[2].toStatus").isEqualTo(OrderStatus.CANCELLED.name());
		assertThat(result).bodyJson().extractingPath("$.history[2].actorName").isEqualTo(staff.getName());
		assertThat(result).bodyJson().extractingPath("$.history[2].actorSide").isEqualTo(ActorSide.RESTAURANT.name());
		assertThat(result).bodyJson()
			.extractingPath("$.history[2].reason")
			.isEqualTo(CancellationReason.ITEM_UNAVAILABLE.name());
		assertThat(result).bodyJson().extractingPath("$.history[2].note").isEqualTo("Acabou o pão");
		assertThat(result).bodyJson()
			.extractingPath("$.history[2].occurredAt")
			.isEqualTo(minutesAfterPlacing(9).toString());
	}

	@Test
	void staffCancelRejectsCustomerReason() {
		User staff = staff("Helena Motivo");
		Order order = orders.create(users.create(), PLACED_AT);

		MvcTestResult result = cancel(staff, order, cancellation(CancellationReason.CHANGED_MIND, null));
		MvcTestResult detail = detail(staff, order);

		assertRefused(result, Messages.INVALID_CANCELLATION_REASON);
		assertThat(detail).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.RECEIVED.name());
		assertThat(detail).bodyJson().extractingPath("$.cancellation").isNull();
	}

	@Test
	void staffCancelOutForDeliveryRequiresDeliveryFailed() {
		User staff = staff("Igor Entrega");
		Order order = orders.create(users.create(), PLACED_AT,
				advancedTo(staff, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY));

		MvcTestResult kitchenReason = cancel(staff, order, cancellation(CancellationReason.ITEM_UNAVAILABLE, null));
		MvcTestResult deliveryFailed = cancel(staff, order, cancellation(CancellationReason.DELIVERY_FAILED, null));

		assertRefused(kitchenReason, Messages.INVALID_CANCELLATION_REASON);
		assertThat(deliveryFailed).hasStatus(HttpStatus.OK);
		assertThat(deliveryFailed).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.CANCELLED.name());
		assertThat(deliveryFailed).bodyJson()
			.extractingPath("$.cancellation.reason")
			.isEqualTo(CancellationReason.DELIVERY_FAILED.name());
		assertThat(deliveryFailed).bodyJson()
			.extractingPath("$.history[3].fromStatus")
			.isEqualTo(OrderStatus.OUT_FOR_DELIVERY.name());
	}

	@Test
	void staffCancelOtherWithoutNoteFailsValidation() {
		User staff = staff("Júlia Observação");
		Order order = orders.create(users.create(), PLACED_AT);

		MvcTestResult blankNote = cancel(staff, order, cancellation(CancellationReason.OTHER, "   "));
		MvcTestResult missingNote = cancel(staff, order, cancellation(CancellationReason.OTHER, null));
		MvcTestResult detail = detail(staff, order);

		assertThat(List.of(blankNote, missingNote)).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
			assertThat(result).bodyJson()
				.extractingPath("$.errors[*].field")
				.asArray()
				.containsExactly(OrderConstants.NOTE_ATTRIBUTE);
			assertThat(result).bodyJson()
				.extractingPath("$.errors[0].message")
				.isEqualTo(Messages.CANCELLATION_NOTE_REQUIRED.text());
		});
		assertThat(detail).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.RECEIVED.name());
	}

	@Test
	void staffApprovesCancellationRequest() {
		User staff = staff("Karina Aprova");
		Order order = orders.create(users.create(), PLACED_AT, advancedTo(staff, OrderStatus.PREPARING));
		CancellationRequest request = cancellationRequests.create(order);
		Instant decidedAt = request.getRequestedAt().plus(MINUTE.multipliedBy(2));
		clock.fixAt(decidedAt);

		MvcTestResult result = approve(staff, order, decision(" Cliente avisado por telefone "));

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.CANCELLED.name());
		assertThat(result).bodyJson().extractingPath("$.cancellation.cancelledBy").isEqualTo(ActorSide.CUSTOMER.name());
		assertThat(result).bodyJson().extractingPath("$.cancellation.reason").isEqualTo(request.getReason().name());
		assertThat(result).bodyJson().extractingPath("$.cancellation.note").isNull();
		assertThat(result).bodyJson().extractingPath("$.cancellationRequest.status").isEqualTo("APPROVED");
		assertThat(result).bodyJson()
			.extractingPath("$.cancellationRequest.reason")
			.isEqualTo(request.getReason().name());
		assertThat(result).bodyJson()
			.extractingPath("$.cancellationRequest.requestedAt")
			.isEqualTo(request.getRequestedAt().toString());
		assertThat(result).bodyJson().extractingPath("$.cancellationRequest.decidedAt").isEqualTo(decidedAt.toString());
		assertThat(result).bodyJson()
			.extractingPath("$.cancellationRequest.decisionNote")
			.isEqualTo("Cliente avisado por telefone");
		assertThat(result).bodyJson().extractingPath("$.cancellationRequest.decidedByName").isEqualTo(staff.getName());
		assertThat(result).bodyJson().extractingPath("$.allowedTransitions").asArray().isEmpty();
		assertThat(result).bodyJson().extractingPath("$.canCancel").isEqualTo(false);
		assertThat(result).bodyJson().extractingPath("$.history[2].fromStatus").isEqualTo(OrderStatus.PREPARING.name());
		assertThat(result).bodyJson().extractingPath("$.history[2].toStatus").isEqualTo(OrderStatus.CANCELLED.name());
		assertThat(result).bodyJson().extractingPath("$.history[2].actorName").isEqualTo(staff.getName());
		assertThat(result).bodyJson().extractingPath("$.history[2].actorSide").isEqualTo(ActorSide.RESTAURANT.name());
		assertThat(result).bodyJson().extractingPath("$.history[2].reason").isEqualTo(request.getReason().name());
		assertThat(result).bodyJson().extractingPath("$.history[2].occurredAt").isEqualTo(decidedAt.toString());
	}

	@Test
	void staffDeclinesCancellationRequestAndOrderAdvancesAgain() {
		User staff = staff("Lucas Recusa");
		Order order = orders.create(users.create(), PLACED_AT, advancedTo(staff, OrderStatus.PREPARING));
		cancellationRequests.create(order);

		MvcTestResult declined = decline(staff, order, EMPTY_BODY);
		MvcTestResult advanced = updateStatus(staff, order, OrderStatus.OUT_FOR_DELIVERY);

		assertThat(declined).hasStatus(HttpStatus.OK);
		assertThat(declined).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.PREPARING.name());
		assertThat(declined).bodyJson().extractingPath("$.cancellationRequest.status").isEqualTo("DECLINED");
		assertThat(declined).bodyJson().extractingPath("$.cancellationRequest.decisionNote").isNull();
		assertThat(declined).bodyJson()
			.extractingPath("$.cancellationRequest.decidedByName")
			.isEqualTo(staff.getName());
		assertThat(declined).bodyJson()
			.extractingPath("$.allowedTransitions")
			.asArray()
			.containsExactly(OrderStatus.OUT_FOR_DELIVERY.name());
		assertThat(declined).bodyJson().extractingPath("$.canCancel").isEqualTo(true);
		assertThat(declined).bodyJson().extractingPath("$.history").asArray().hasSize(2);
		assertThat(advanced).hasStatus(HttpStatus.OK);
		assertThat(advanced).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.OUT_FOR_DELIVERY.name());
		assertThat(advanced).bodyJson().extractingPath("$.cancellationRequest.status").isEqualTo("DECLINED");
	}

	@Test
	void decidingWithoutPendingRequestIsRejected() {
		User staff = staff("Marina Decisão");
		Order withoutRequest = orders.create(users.create(), PLACED_AT, advancedTo(staff, OrderStatus.PREPARING));
		Order alreadyDeclined = orders.create(users.create(), PLACED_AT, advancedTo(staff, OrderStatus.PREPARING));
		cancellationRequests.create(alreadyDeclined,
				request -> request.decline(staff, null, request.getRequestedAt().plus(MINUTE)));

		MvcTestResult approveWithout = approve(staff, withoutRequest, EMPTY_BODY);
		MvcTestResult declineWithout = decline(staff, withoutRequest, EMPTY_BODY);
		MvcTestResult approveDeclined = approve(staff, alreadyDeclined, EMPTY_BODY);
		MvcTestResult detail = detail(staff, alreadyDeclined);

		assertThat(List.of(approveWithout, declineWithout, approveDeclined))
			.allSatisfy(result -> assertRefused(result, Messages.NO_PENDING_CANCELLATION_REQUEST));
		assertThat(detail).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.PREPARING.name());
		assertThat(detail).bodyJson().extractingPath("$.cancellationRequest.status").isEqualTo("DECLINED");
	}

	@Test
	void pendingRequestBlocksAdvance() {
		User staff = staff("Nina Bloqueio");
		Order order = orders.create(users.create(), PLACED_AT, advancedTo(staff, OrderStatus.PREPARING));
		CancellationRequest request = cancellationRequests.create(order);

		MvcTestResult advance = updateStatus(staff, order, OrderStatus.OUT_FOR_DELIVERY);
		MvcTestResult cancel = cancel(staff, order, cancellation(CancellationReason.ITEM_UNAVAILABLE, null));
		MvcTestResult detail = detail(staff, order);

		assertRefused(advance, Messages.PENDING_CANCELLATION_REQUEST);
		assertRefused(cancel, Messages.PENDING_CANCELLATION_REQUEST);
		assertThat(detail).bodyJson().extractingPath("$.status").isEqualTo(OrderStatus.PREPARING.name());
		assertThat(detail).bodyJson().extractingPath("$.allowedTransitions").asArray().isEmpty();
		assertThat(detail).bodyJson().extractingPath("$.canCancel").isEqualTo(false);
		assertThat(detail).bodyJson().extractingPath("$.cancellationReasons").asArray().isEmpty();
		assertThat(detail).bodyJson().extractingPath("$.cancellationRequest.status").isEqualTo("PENDING");
		assertThat(detail).bodyJson()
			.extractingPath("$.cancellationRequest.reason")
			.isEqualTo(request.getReason().name());
		assertThat(detail).bodyJson()
			.extractingPath("$.cancellationRequest.requestedAt")
			.isEqualTo(request.getRequestedAt().toString());
		assertThat(detail).bodyJson().extractingPath("$.cancellationRequest.decidedByName").isNull();
	}

	@Test
	void customerCannotUseStaffCommands() {
		User customer = users.create();
		Order order = orders.create(customer, PLACED_AT);

		MvcTestResult advance = updateStatus(customer, order, OrderStatus.PREPARING);
		MvcTestResult cancel = cancel(customer, order, cancellation(CancellationReason.CHANGED_MIND, null));
		MvcTestResult approve = approve(customer, order, EMPTY_BODY);
		MvcTestResult decline = decline(customer, order, EMPTY_BODY);

		assertThat(List.of(advance, cancel, approve, decline)).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
		});
	}

	@Test
	void staffCommandsOnUnknownOrderAreNotFound() {
		User staff = staff("Otávio Perdido");
		UUID unknown = UUID.randomUUID();

		MvcTestResult advance = send(staff, mvc.patch().uri(STATUS_PATH, unknown), status(OrderStatus.PREPARING));
		MvcTestResult cancel = send(staff, mvc.post().uri(CANCEL_PATH, unknown),
				cancellation(CancellationReason.STORE_CLOSED, null));
		MvcTestResult approve = send(staff, mvc.post().uri(APPROVE_PATH, unknown), EMPTY_BODY);
		MvcTestResult decline = send(staff, mvc.post().uri(DECLINE_PATH, unknown), EMPTY_BODY);

		assertThat(List.of(advance, cancel, approve, decline)).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
			assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.ORDER_NOT_FOUND.name());
		});
	}

	@Test
	void boardListsFlagPendingRequestsWithoutNPlusOne() {
		User staff = staff("Paula Quadro");
		String fewMarker = marker();
		String manyMarker = marker();
		User fewCustomer = customer(fewMarker + " Bia Poucos");
		User manyCustomer = customer(manyMarker + " Caio Muitos");
		cancellationRequests.create(preparing(fewCustomer, staff, 1));
		orders.create(fewCustomer, minutesAfterPlacing(2));
		List<Order> pending = List.of(preparing(manyCustomer, staff, 3), preparing(manyCustomer, staff, 4),
				preparing(manyCustomer, staff, 5));
		pending.forEach(cancellationRequests::create);
		Order declined = preparing(manyCustomer, staff, 6);
		cancellationRequests.create(declined,
				request -> request.decline(staff, null, request.getRequestedAt().plus(MINUTE)));
		Order outForDelivery = orders.create(manyCustomer, minutesAfterPlacing(7),
				advancedTo(staff, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY));
		Order received = orders.create(manyCustomer, minutesAfterPlacing(8));
		statistics().setStatisticsEnabled(true);

		CountedResult few = listCountingStatements(staff, fewMarker);
		CountedResult many = listCountingStatements(staff, manyMarker);

		assertThat(few.result()).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(2);
		assertThat(many.result()).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(6);
		assertThat(many.statements()).isPositive().isEqualTo(few.statements());
		assertThat(many.result()).bodyJson()
			.extractingPath("$.data[?(@.hasPendingCancellationRequest == true)].id")
			.asArray()
			.containsExactlyInAnyOrderElementsOf(pending.stream().map(this::idOf).toList());
		assertThat(many.result()).bodyJson()
			.extractingPath("$.data[?(@.hasPendingCancellationRequest == true)].canCancel")
			.asArray()
			.containsOnly(false);
		assertThat(many.result()).bodyJson()
			.extractingPath("$.data[?(@.hasPendingCancellationRequest == true)].allowedTransitions[*]")
			.asArray()
			.isEmpty();
		assertThat(many.result()).bodyJson()
			.extractingPath("$.data[?(@.hasPendingCancellationRequest == true)].cancellationReasons[*]")
			.asArray()
			.isEmpty();
		assertThat(many.result()).bodyJson()
			.extractingPath("$.data[?(@.id == '%s')].allowedTransitions[*]".formatted(idOf(declined)))
			.asArray()
			.containsExactly(OrderStatus.OUT_FOR_DELIVERY.name());
		assertThat(many.result()).bodyJson()
			.extractingPath("$.data[?(@.id == '%s')].cancellationReasons[*]".formatted(idOf(outForDelivery)))
			.asArray()
			.containsExactly(CancellationReason.DELIVERY_FAILED.name());
		assertThat(many.result()).bodyJson()
			.extractingPath("$.data[?(@.id == '%s')].canCancel".formatted(idOf(received)))
			.asArray()
			.containsExactly(true);
		assertThat(few.result()).bodyJson()
			.extractingPath("$.data[*].hasPendingCancellationRequest")
			.asArray()
			.containsExactlyInAnyOrder(true, false);
	}

	private Order preparing(User customer, User staff, int minutesAfterOpening) {
		return orders.create(customer, minutesAfterPlacing(minutesAfterOpening),
				advancedTo(staff, OrderStatus.PREPARING));
	}

	private CountedResult listCountingStatements(User staff, String search) {
		Statistics statistics = statistics();
		statistics.clear();
		MvcTestResult result = mvc.get()
			.uri(OrderConstants.ORDERS_PATH)
			.param("search", search)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(staff))
			.exchange();
		return new CountedResult(result, statistics.getPrepareStatementCount());
	}

	private Statistics statistics() {
		return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
	}

	private Consumer<Order> advancedTo(User staff, OrderStatus... statuses) {
		return order -> {
			Instant occurredAt = order.getPlacedAt();
			for (OrderStatus target : statuses) {
				occurredAt = occurredAt.plus(MINUTE);
				order.advance(target, staff, ActorSide.RESTAURANT, occurredAt);
			}
		};
	}

	private MvcTestResult updateStatusAt(User caller, Order order, OrderStatus target, Instant at) {
		clock.fixAt(at);
		return updateStatus(caller, order, target);
	}

	private MvcTestResult updateStatus(User caller, Order order, OrderStatus target) {
		return send(caller, mvc.patch().uri(STATUS_PATH, order.getId()), status(target));
	}

	private MvcTestResult cancel(User caller, Order order, String body) {
		return send(caller, mvc.post().uri(CANCEL_PATH, order.getId()), body);
	}

	private MvcTestResult approve(User caller, Order order, String body) {
		return send(caller, mvc.post().uri(APPROVE_PATH, order.getId()), body);
	}

	private MvcTestResult decline(User caller, Order order, String body) {
		return send(caller, mvc.post().uri(DECLINE_PATH, order.getId()), body);
	}

	private MvcTestResult detail(User caller, Order order) {
		return mvc.get()
			.uri(ORDER_PATH, order.getId())
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
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo(code.text());
	}

	private Instant minutesAfterPlacing(int minutes) {
		return PLACED_AT.plus(MINUTE.multipliedBy(minutes));
	}

	private User staff(String name) {
		String unique = name + " " + marker();
		return users.create(unique, STAFF_EMAIL_PREFIX + UUID.randomUUID() + EMAIL_DOMAIN,
				user -> user.changeRole(Role.STAFF));
	}

	private User customer(String name) {
		return users.create(name, name.toLowerCase(Locale.ROOT).replace(" ", ".") + EMAIL_DOMAIN);
	}

	private String idOf(Order order) {
		return order.getId().toString();
	}

	private String marker() {
		return "lif" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

	record CountedResult(MvcTestResult result, long statements) {
	}

}
