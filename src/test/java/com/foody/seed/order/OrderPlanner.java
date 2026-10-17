package com.foody.seed.order;

import com.foody.api.order.entity.CancellationRequestStatus;
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
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.datafaker.Faker;
import org.jspecify.annotations.Nullable;

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
		int fixedCustomerRecent = (int) recent.stream().filter(OrderPlan::forFixedCustomer).count();
		int olderCount = Math.max(0, scale.of(OrderSeedConstants.ORDER_BASE_COUNT) - recent.size());
		List<OrderPlan> older = olderPlans(scale, now, olderCount,
				Math.max(0, OrderSeedConstants.FIXED_CUSTOMER_ORDER_COUNT - fixedCustomerRecent));
		return Stream.concat(recent.stream(), older.stream())
			.sorted(Comparator.comparing(OrderPlan::placedAt))
			.toList();
	}

	private List<OrderPlan> recentPlans(RecentOrders slot, SeedScale scale, Instant now) {
		return IntStream.range(0, slot.countAt(scale))
			.mapToObj(index -> new OrderPlan(now.minus(slot.age().draw(faker)), slot.status(),
					index == 0 && slot.holdsFixedCustomerOrder(), slot.cancellationRequest()))
			.toList();
	}

	private List<OrderPlan> olderPlans(SeedScale scale, Instant now, int count, int fixedCustomerCount) {
		int approved = scale.of(OrderSeedConstants.APPROVED_REQUESTS_BASE);
		int declined = scale.of(OrderSeedConstants.DECLINED_REQUESTS_BASE);
		int regular = Math.max(0, count - fixedCustomerCount - approved - declined);
		return Stream.of(Stream.generate(() -> olderPlan(now, olderOutcome(), true, null)).limit(fixedCustomerCount),
				Stream.generate(() -> olderPlan(now, OrderStatus.CANCELLED, false, CancellationRequestStatus.APPROVED))
					.limit(approved),
				Stream.generate(() -> olderPlan(now, OrderStatus.DELIVERED, false, CancellationRequestStatus.DECLINED))
					.limit(declined),
				Stream.generate(() -> olderPlan(now, olderOutcome(), false, null)).limit(regular))
			.flatMap(Function.identity())
			.toList();
	}

	private OrderPlan olderPlan(Instant now, OrderStatus outcome, boolean forFixedCustomer,
			@Nullable CancellationRequestStatus cancellationRequest) {
		Instant latest = now.minus(OrderSeedConstants.RECENT_WINDOW);
		LocalDate firstCandidate = LocalDate.ofInstant(now, zone)
			.minusDays(faker.number().numberBetween(0, OrderSeedConstants.HISTORY_DAYS));
		LocalTime time = DayPart.drawTime(faker);
		Instant placedAt = Stream.iterate(firstCandidate, day -> day.minusDays(1))
			.map(day -> day.atTime(time).atZone(zone).toInstant())
			.filter(candidate -> !candidate.isAfter(latest))
			.findFirst()
			.orElseThrow();
		return new OrderPlan(placedAt, outcome, forFixedCustomer, cancellationRequest);
	}

	private OrderStatus olderOutcome() {
		if (faker.number().numberBetween(0, SeedConstants.PERCENT) < OrderSeedConstants.DELIVERED_PERCENT) {
			return OrderStatus.DELIVERED;
		}
		return OrderStatus.CANCELLED;
	}

}
