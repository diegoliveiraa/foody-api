package com.foody.seed.order;

import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.OrderStatus;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.datafaker.Faker;

public enum SeedCancellation {

	CUSTOMER_GAVE_UP(OrderStatus.RECEIVED, ActorSide.CUSTOMER, OrderSeedConstants.CUSTOMER_REASONS,
			OrderSeedConstants.CUSTOMER_OTHER_NOTES, new MinuteRange(2, 10), 40),
	RESTAURANT_REFUSED(OrderStatus.RECEIVED, ActorSide.RESTAURANT,
			List.of(CancellationReason.ITEM_UNAVAILABLE, CancellationReason.STORE_CLOSED,
					CancellationReason.CUSTOMER_REQUEST),
			OrderSeedConstants.RESTAURANT_OTHER_NOTES, new MinuteRange(1, 5), 25),
	KITCHEN_STOPPED(OrderStatus.PREPARING, ActorSide.RESTAURANT,
			List.of(CancellationReason.ITEM_UNAVAILABLE, CancellationReason.CUSTOMER_REQUEST),
			OrderSeedConstants.RESTAURANT_OTHER_NOTES, new MinuteRange(5, 15), 20),
	DELIVERY_FAILED(OrderStatus.OUT_FOR_DELIVERY, ActorSide.RESTAURANT, List.of(CancellationReason.DELIVERY_FAILED),
			List.of(), new MinuteRange(15, 35), 15);

	private static final List<SeedCancellation> BY_WEIGHT = Arrays.stream(values())
		.flatMap(scenario -> Collections.nCopies(scenario.weight, scenario).stream())
		.toList();

	private final OrderStatus stage;

	private final ActorSide side;

	private final List<CancellationReason> reasons;

	private final List<String> otherNotes;

	private final MinuteRange delay;

	private final int weight;

	SeedCancellation(OrderStatus stage, ActorSide side, List<CancellationReason> reasons, List<String> otherNotes,
			MinuteRange delay, int weight) {
		this.stage = stage;
		this.side = side;
		this.reasons = reasons;
		this.otherNotes = otherNotes;
		this.delay = delay;
		this.weight = weight;
	}

	public static SeedCancellation draw(Faker faker) {
		return faker.options().nextElement(BY_WEIGHT);
	}

	public OrderStatus stage() {
		return stage;
	}

	public ActorSide side() {
		return side;
	}

	public List<CancellationReason> reasons() {
		return reasons;
	}

	public List<String> otherNotes() {
		return otherNotes;
	}

	public MinuteRange delay() {
		return delay;
	}

}
