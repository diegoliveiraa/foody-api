package com.foody.seed.order;

import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.CancellationRequest;
import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.user.entity.User;
import com.foody.seed.SeedConstants;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.datafaker.Faker;

public class OrderStory {

	private final Faker faker;

	private final Map<CancellationRequestStatus, RequestDecision> decisions;

	public OrderStory(Faker faker) {
		this.faker = faker;
		this.decisions = Map.of(CancellationRequestStatus.PENDING, this::leavePending,
				CancellationRequestStatus.APPROVED, this::approve, CancellationRequestStatus.DECLINED, this::decline);
	}

	public Optional<CancellationRequest> playOut(Order order, OrderPlan plan, User customer, List<User> staff) {
		CancellationRequestStatus decision = plan.cancellationRequest();
		if (decision == null) {
			playOut(order, plan.outcome(), customer, staff);
			return Optional.empty();
		}
		return Optional.of(requestCancellation(order, plan.outcome(), decision, staff));
	}

	private void playOut(Order order, OrderStatus outcome, User customer, List<User> staff) {
		if (outcome == OrderStatus.CANCELLED) {
			cancel(order, customer, staff);
			return;
		}
		advanceTo(order, outcome, staff, order.getPlacedAt());
	}

	private Instant advanceTo(Order order, OrderStatus target, List<User> staff, Instant since) {
		List<OrderStatus> path = OrderSeedConstants.FORWARD_PATH;
		List<OrderStatus> steps = path.subList(path.indexOf(order.getStatus()) + 1, path.indexOf(target) + 1);
		Instant occurredAt = since;
		for (OrderStatus step : steps) {
			occurredAt = occurredAt.plus(OrderSeedConstants.STEP_DELAYS.get(step).draw(faker));
			order.advance(step, faker.options().nextElement(staff), ActorSide.RESTAURANT, occurredAt);
		}
		return occurredAt;
	}

	private void cancel(Order order, User customer, List<User> staff) {
		SeedCancellation scenario = SeedCancellation.draw(faker);
		Instant stageReachedAt = advanceTo(order, scenario.stage(), staff, order.getPlacedAt());
		Instant cancelledAt = stageReachedAt.plus(scenario.delay().draw(faker));
		User actor = scenario.side() == ActorSide.CUSTOMER ? customer : faker.options().nextElement(staff);
		if (explainsWithNote(scenario)) {
			order.cancel(actor, scenario.side(), CancellationReason.OTHER,
					faker.options().nextElement(scenario.otherNotes()), cancelledAt);
			return;
		}
		order.cancel(actor, scenario.side(), faker.options().nextElement(scenario.reasons()), null, cancelledAt);
	}

	private boolean explainsWithNote(SeedCancellation scenario) {
		return !scenario.otherNotes().isEmpty() && drawsOtherReason();
	}

	private CancellationRequest requestCancellation(Order order, OrderStatus outcome,
			CancellationRequestStatus decision, List<User> staff) {
		Instant preparingAt = advanceTo(order, OrderStatus.PREPARING, staff, order.getPlacedAt());
		CancellationRequest request = customerRequest(order,
				preparingAt.plus(OrderSeedConstants.REQUEST_DELAY.draw(faker)));
		decisions.get(decision).decide(order, request, outcome, staff);
		return request;
	}

	private CancellationRequest customerRequest(Order order, Instant requestedAt) {
		if (drawsOtherReason()) {
			return new CancellationRequest(order, CancellationReason.OTHER,
					faker.options().nextElement(OrderSeedConstants.CUSTOMER_OTHER_NOTES), requestedAt);
		}
		return new CancellationRequest(order, faker.options().nextElement(OrderSeedConstants.CUSTOMER_REASONS), null,
				requestedAt);
	}

	private void leavePending(Order order, CancellationRequest request, OrderStatus outcome, List<User> staff) {
	}

	private void approve(Order order, CancellationRequest request, OrderStatus outcome, List<User> staff) {
		User decider = faker.options().nextElement(staff);
		Instant decidedAt = decisionTimeOf(request);
		request.approve(decider, null, decidedAt);
		order.cancel(decider, ActorSide.RESTAURANT, ActorSide.CUSTOMER, request.getReason(), request.getNote(),
				decidedAt);
	}

	private void decline(Order order, CancellationRequest request, OrderStatus outcome, List<User> staff) {
		User decider = faker.options().nextElement(staff);
		Instant decidedAt = decisionTimeOf(request);
		request.decline(decider, faker.options().nextElement(OrderSeedConstants.DECLINE_NOTES), decidedAt);
		advanceTo(order, outcome, staff, decidedAt);
	}

	private Instant decisionTimeOf(CancellationRequest request) {
		return request.getRequestedAt().plus(OrderSeedConstants.DECISION_DELAY.draw(faker));
	}

	private boolean drawsOtherReason() {
		return faker.number().numberBetween(0, SeedConstants.PERCENT) < OrderSeedConstants.OTHER_REASON_PERCENT;
	}

	@FunctionalInterface
	private interface RequestDecision {

		void decide(Order order, CancellationRequest request, OrderStatus outcome, List<User> staff);

	}

}
