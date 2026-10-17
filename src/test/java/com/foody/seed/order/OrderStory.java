package com.foody.seed.order;

import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.user.entity.User;
import com.foody.seed.SeedConstants;
import java.time.Instant;
import java.util.List;
import net.datafaker.Faker;

public class OrderStory {

	private final Faker faker;

	public OrderStory(Faker faker) {
		this.faker = faker;
	}

	public void playOut(Order order, OrderStatus outcome, User customer, List<User> staff) {
		if (outcome == OrderStatus.CANCELLED) {
			cancel(order, customer, staff);
			return;
		}
		advanceTo(order, outcome, staff);
	}

	private Instant advanceTo(Order order, OrderStatus target, List<User> staff) {
		List<OrderStatus> steps = OrderSeedConstants.FORWARD_PATH.subList(0,
				OrderSeedConstants.FORWARD_PATH.indexOf(target) + 1);
		Instant occurredAt = order.getPlacedAt();
		for (OrderStatus step : steps) {
			occurredAt = occurredAt.plus(OrderSeedConstants.STEP_DELAYS.get(step).draw(faker));
			order.advance(step, faker.options().nextElement(staff), ActorSide.RESTAURANT, occurredAt);
		}
		return occurredAt;
	}

	private void cancel(Order order, User customer, List<User> staff) {
		SeedCancellation scenario = SeedCancellation.draw(faker);
		Instant stageReachedAt = advanceTo(order, scenario.stage(), staff);
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
		return !scenario.otherNotes().isEmpty()
				&& faker.number().numberBetween(0, SeedConstants.PERCENT) < OrderSeedConstants.OTHER_REASON_PERCENT;
	}

}
