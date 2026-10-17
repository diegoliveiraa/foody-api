package com.foody.seed.order;

import com.foody.api.order.entity.OrderStatus;
import com.foody.seed.SeedConstants;
import com.foody.seed.SeedScale;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.datafaker.Faker;

public class OrderPlanner {

	private final Faker faker;

	private final ZoneId zone;

	public OrderPlanner(Faker faker, ZoneId zone) {
		this.faker = faker;
		this.zone = zone;
	}

	public List<OrderPlan> plan(SeedScale scale, Instant now) {
		List<OrderPlan> recent = Arrays.stream(RecentOrders.values())
			.flatMap(slot -> recentPlans(slot, scale, now).stream())
			.toList();
		int olderCount = Math.max(0, scale.of(OrderSeedConstants.ORDER_BASE_COUNT) - recent.size());
		List<OrderPlan> older = IntStream.range(0, olderCount)
			.mapToObj(index -> olderPlan(now, index < OrderSeedConstants.FIXED_CUSTOMER_ORDER_COUNT - 1))
			.toList();
		return Stream.concat(recent.stream(), older.stream())
			.sorted(Comparator.comparing(OrderPlan::placedAt))
			.toList();
	}

	private List<OrderPlan> recentPlans(RecentOrders slot, SeedScale scale, Instant now) {
		return IntStream.range(0, slot.countAt(scale))
			.mapToObj(index -> new OrderPlan(now.minus(slot.age().draw(faker)), slot.status(),
					index == 0 && slot.holdsFixedCustomerOrder()))
			.toList();
	}

	private OrderPlan olderPlan(Instant now, boolean forFixedCustomer) {
		Instant latest = now.minus(OrderSeedConstants.RECENT_WINDOW);
		LocalDate firstCandidate = LocalDate.ofInstant(now, zone)
			.minusDays(faker.number().numberBetween(0, OrderSeedConstants.HISTORY_DAYS));
		LocalTime time = DayPart.drawTime(faker);
		Instant placedAt = Stream.iterate(firstCandidate, day -> day.minusDays(1))
			.map(day -> day.atTime(time).atZone(zone).toInstant())
			.filter(candidate -> !candidate.isAfter(latest))
			.findFirst()
			.orElseThrow();
		return new OrderPlan(placedAt, olderOutcome(), forFixedCustomer);
	}

	private OrderStatus olderOutcome() {
		if (faker.number().numberBetween(0, SeedConstants.PERCENT) < OrderSeedConstants.DELIVERED_PERCENT) {
			return OrderStatus.DELIVERED;
		}
		return OrderStatus.CANCELLED;
	}

}
