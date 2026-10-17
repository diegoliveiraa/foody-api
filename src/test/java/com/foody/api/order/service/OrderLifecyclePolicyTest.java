package com.foody.api.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.foody.api.order.dto.CustomerOrderActions;
import com.foody.api.order.dto.StaffOrderActions;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.shared.exception.DomainException;
import com.foody.api.shared.message.Messages;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

class OrderLifecyclePolicyTest {

	private static final List<CancellationRequestStatus> REQUEST_STATES = Arrays.asList(null,
			CancellationRequestStatus.PENDING, CancellationRequestStatus.APPROVED, CancellationRequestStatus.DECLINED);

	private static final List<OrderStatus> ACTIVE_STATUSES = List.of(OrderStatus.RECEIVED, OrderStatus.PREPARING,
			OrderStatus.OUT_FOR_DELIVERY);

	private static final List<OrderStatus> FINAL_STATUSES = List.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);

	private static final Map<OrderStatus, OrderStatus> NEXT = Map.of(OrderStatus.RECEIVED, OrderStatus.PREPARING,
			OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);

	private static final List<CancellationReason> CUSTOMER_REASONS = List.of(CancellationReason.CHANGED_MIND,
			CancellationReason.ORDERED_BY_MISTAKE, CancellationReason.TAKING_TOO_LONG, CancellationReason.OTHER);

	private static final List<CancellationReason> KITCHEN_REASONS = List.of(CancellationReason.ITEM_UNAVAILABLE,
			CancellationReason.STORE_CLOSED, CancellationReason.CUSTOMER_REQUEST, CancellationReason.OTHER);

	private static final Map<OrderStatus, List<CancellationReason>> STAFF_REASONS = Map.of(OrderStatus.RECEIVED,
			KITCHEN_REASONS, OrderStatus.PREPARING, KITCHEN_REASONS, OrderStatus.OUT_FOR_DELIVERY,
			List.of(CancellationReason.DELIVERY_FAILED));

	private static final Map<OrderStatus, List<CancellationReason>> CUSTOMER_CANCEL_REASONS = Map
		.of(OrderStatus.RECEIVED, CUSTOMER_REASONS);

	private static final Map<OrderStatus, List<CancellationReason>> CUSTOMER_REQUEST_REASONS = Map
		.of(OrderStatus.PREPARING, CUSTOMER_REASONS);

	private final OrderLifecyclePolicy policy = new OrderLifecyclePolicy();

	@ParameterizedTest
	@MethodSource("statusPairs")
	void lifecyclePolicyAllowsOnlyTheNextStatus(OrderStatus status, OrderStatus target) {
		Optional<Messages> expected = NEXT.get(status) == target ? Optional.empty()
				: Optional.of(Messages.INVALID_STATUS_TRANSITION);
		List<OrderStatus> expectedTransitions = Optional.ofNullable(NEXT.get(status)).stream().toList();

		Optional<Messages> refusal = refusalOf(() -> policy.requireTransition(status, null, target));
		StaffOrderActions actions = policy.staffActions(status, null);

		assertThat(refusal).isEqualTo(expected);
		assertThat(actions.allowedTransitions()).isEqualTo(expectedTransitions);
	}

	@ParameterizedTest
	@MethodSource("activeStatusesAndRequestStates")
	void pendingRequestBlocksAdvanceAndStaffCancel(OrderStatus status, @Nullable CancellationRequestStatus request) {
		boolean pending = request == CancellationRequestStatus.PENDING;
		Optional<Messages> expected = pending ? Optional.of(Messages.PENDING_CANCELLATION_REQUEST) : Optional.empty();
		CancellationReason staffReason = STAFF_REASONS.get(status).getFirst();

		Optional<Messages> advance = refusalOf(() -> policy.requireTransition(status, request, NEXT.get(status)));
		Optional<Messages> cancel = refusalOf(
				() -> policy.requireCancellation(ActorSide.RESTAURANT, status, request, staffReason));
		StaffOrderActions actions = policy.staffActions(status, request);
		boolean flaggedPending = policy.hasPendingRequest(request);

		assertThat(advance).isEqualTo(expected);
		assertThat(cancel).isEqualTo(expected);
		assertThat(actions.allowedTransitions()).isEqualTo(pending ? List.of() : List.of(NEXT.get(status)));
		assertThat(actions.canCancel()).isEqualTo(!pending);
		assertThat(actions.cancellationReasons()).isEqualTo(pending ? List.of() : STAFF_REASONS.get(status));
		assertThat(flaggedPending).isEqualTo(pending);
	}

	@ParameterizedTest
	@EnumSource(CancellationReason.class)
	void outForDeliveryCancelsOnlyWithDeliveryFailed(CancellationReason reason) {
		Optional<Messages> expected = reason == CancellationReason.DELIVERY_FAILED ? Optional.empty()
				: Optional.of(Messages.INVALID_CANCELLATION_REASON);

		Optional<Messages> staff = refusalOf(
				() -> policy.requireCancellation(ActorSide.RESTAURANT, OrderStatus.OUT_FOR_DELIVERY, null, reason));
		Optional<Messages> customer = refusalOf(
				() -> policy.requireCancellation(ActorSide.CUSTOMER, OrderStatus.OUT_FOR_DELIVERY, null, reason));
		StaffOrderActions actions = policy.staffActions(OrderStatus.OUT_FOR_DELIVERY, null);

		assertThat(staff).isEqualTo(expected);
		assertThat(customer).contains(Messages.CANCELLATION_NOT_ALLOWED);
		assertThat(actions.canCancel()).isTrue();
		assertThat(actions.cancellationReasons()).containsExactly(CancellationReason.DELIVERY_FAILED);
	}

	@ParameterizedTest
	@MethodSource("statusesAndRequestStates")
	void customerCancelsOnlyWhileReceived(OrderStatus status, @Nullable CancellationRequestStatus request) {
		boolean allowed = status == OrderStatus.RECEIVED && request != CancellationRequestStatus.PENDING;
		Optional<Messages> expected = allowed ? Optional.empty() : Optional.of(Messages.CANCELLATION_NOT_ALLOWED);

		Optional<Messages> refusal = refusalOf(
				() -> policy.requireCancellation(ActorSide.CUSTOMER, status, request, CancellationReason.CHANGED_MIND));
		CustomerOrderActions actions = policy.customerActions(status, request);

		assertThat(refusal).isEqualTo(expected);
		assertThat(actions.canCancel()).isEqualTo(allowed);
	}

	@ParameterizedTest
	@MethodSource("statusesAndRequestStates")
	void customerRequestsOnlyWhilePreparingAndOnce(OrderStatus status, @Nullable CancellationRequestStatus request) {
		Optional<Messages> expected = expectedRequestRefusal(status, request);

		Optional<Messages> refusal = refusalOf(
				() -> policy.requireCancellationRequest(status, request, CancellationReason.TAKING_TOO_LONG));
		CustomerOrderActions actions = policy.customerActions(status, request);

		assertThat(refusal).isEqualTo(expected);
		assertThat(actions.canRequestCancellation()).isEqualTo(expected.isEmpty());
	}

	@ParameterizedTest
	@MethodSource("finalStatusesSidesAndRequestStates")
	void finalStatusesAllowNothing(OrderStatus status, ActorSide side, @Nullable CancellationRequestStatus request) {
		List<OrderStatus> targets = List.of(OrderStatus.values());
		List<CancellationReason> reasons = List.of(CancellationReason.values());

		StaffOrderActions staff = policy.staffActions(status, request);
		CustomerOrderActions customer = policy.customerActions(status, request);
		List<Optional<Messages>> transitions = targets.stream()
			.map(target -> refusalOf(() -> policy.requireTransition(status, request, target)))
			.toList();
		List<Optional<Messages>> cancellations = reasons.stream()
			.map(reason -> refusalOf(() -> policy.requireCancellation(side, status, request, reason)))
			.toList();
		List<Optional<Messages>> requests = reasons.stream()
			.map(reason -> refusalOf(() -> policy.requireCancellationRequest(status, request, reason)))
			.toList();

		assertThat(staff.allowedTransitions()).isEmpty();
		assertThat(staff.canCancel()).isFalse();
		assertThat(staff.cancellationReasons()).isEmpty();
		assertThat(customer.canCancel()).isFalse();
		assertThat(customer.canRequestCancellation()).isFalse();
		assertThat(customer.cancellationReasons()).isEmpty();
		assertThat(transitions).containsOnly(Optional.of(Messages.INVALID_STATUS_TRANSITION));
		assertThat(cancellations).containsOnly(Optional.of(Messages.CANCELLATION_NOT_ALLOWED));
		assertThat(requests).containsOnly(Optional.of(Messages.CANCELLATION_NOT_ALLOWED));
	}

	@ParameterizedTest
	@MethodSource("statusesAndReasons")
	void cancellationReasonsMatchSideAndStatus(OrderStatus status, CancellationReason reason) {
		List<CancellationReason> staffReasons = STAFF_REASONS.getOrDefault(status, List.of());
		List<CancellationReason> customerCancelReasons = CUSTOMER_CANCEL_REASONS.getOrDefault(status, List.of());
		List<CancellationReason> customerRequestReasons = CUSTOMER_REQUEST_REASONS.getOrDefault(status, List.of());
		List<CancellationReason> customerReasons = Stream.of(customerCancelReasons, customerRequestReasons)
			.flatMap(List::stream)
			.toList();

		StaffOrderActions staff = policy.staffActions(status, null);
		CustomerOrderActions customer = policy.customerActions(status, null);
		Optional<Messages> staffCancel = refusalOf(
				() -> policy.requireCancellation(ActorSide.RESTAURANT, status, null, reason));
		Optional<Messages> customerCancel = refusalOf(
				() -> policy.requireCancellation(ActorSide.CUSTOMER, status, null, reason));
		Optional<Messages> customerRequest = refusalOf(() -> policy.requireCancellationRequest(status, null, reason));

		assertThat(staff.cancellationReasons()).isEqualTo(staffReasons);
		assertThat(customer.cancellationReasons()).isEqualTo(customerReasons);
		assertThat(staffCancel).isEqualTo(expectedReasonRefusal(staffReasons, reason));
		assertThat(customerCancel).isEqualTo(expectedReasonRefusal(customerCancelReasons, reason));
		assertThat(customerRequest).isEqualTo(expectedReasonRefusal(customerRequestReasons, reason));
	}

	@ParameterizedTest
	@NullSource
	@EnumSource(CancellationRequestStatus.class)
	void onlyAPendingRequestCanBeDecided(@Nullable CancellationRequestStatus request) {
		Optional<Messages> expected = request == CancellationRequestStatus.PENDING ? Optional.empty()
				: Optional.of(Messages.NO_PENDING_CANCELLATION_REQUEST);

		Optional<Messages> refusal = refusalOf(() -> policy.requireDecision(request));

		assertThat(refusal).isEqualTo(expected);
	}

	static Stream<Arguments> statusPairs() {
		return Arrays.stream(OrderStatus.values())
			.flatMap(status -> Arrays.stream(OrderStatus.values()).map(target -> Arguments.of(status, target)));
	}

	static Stream<Arguments> activeStatusesAndRequestStates() {
		return ACTIVE_STATUSES.stream()
			.flatMap(status -> REQUEST_STATES.stream().map(request -> Arguments.of(status, request)));
	}

	static Stream<Arguments> statusesAndRequestStates() {
		return Arrays.stream(OrderStatus.values())
			.flatMap(status -> REQUEST_STATES.stream().map(request -> Arguments.of(status, request)));
	}

	static Stream<Arguments> finalStatusesSidesAndRequestStates() {
		return FINAL_STATUSES.stream()
			.flatMap(status -> Arrays.stream(ActorSide.values())
				.flatMap(side -> REQUEST_STATES.stream().map(request -> Arguments.of(status, side, request))));
	}

	static Stream<Arguments> statusesAndReasons() {
		return Arrays.stream(OrderStatus.values())
			.flatMap(status -> Arrays.stream(CancellationReason.values()).map(reason -> Arguments.of(status, reason)));
	}

	private Optional<Messages> expectedRequestRefusal(OrderStatus status, @Nullable CancellationRequestStatus request) {
		if (status != OrderStatus.PREPARING) {
			return Optional.of(Messages.CANCELLATION_NOT_ALLOWED);
		}
		if (request != null) {
			return Optional.of(Messages.CANCELLATION_ALREADY_REQUESTED);
		}
		return Optional.empty();
	}

	private Optional<Messages> expectedReasonRefusal(List<CancellationReason> allowed, CancellationReason reason) {
		if (allowed.isEmpty()) {
			return Optional.of(Messages.CANCELLATION_NOT_ALLOWED);
		}
		if (allowed.contains(reason)) {
			return Optional.empty();
		}
		return Optional.of(Messages.INVALID_CANCELLATION_REASON);
	}

	private Optional<Messages> refusalOf(ThrowingCallable command) {
		return Optional.ofNullable(catchThrowable(command))
			.map(DomainException.class::cast)
			.map(DomainException::messageKey);
	}

}
