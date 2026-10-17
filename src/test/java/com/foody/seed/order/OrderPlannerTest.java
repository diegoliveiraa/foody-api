package com.foody.seed.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.order.entity.OrderStatus;
import com.foody.seed.SeedConstants;
import com.foody.seed.SeedScale;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrderPlannerTest {

	private static final Instant NOW = Instant.parse("2026-10-17T22:30:00Z");

	private static final long RANDOM_SEED = 42;

	private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

	@ParameterizedTest
	@EnumSource(SeedScale.class)
	void activeOrdersStayWithinTheCapAtEveryScale(SeedScale scale) {
		OrderPlanner planner = new OrderPlanner(new Faker(SeedConstants.LOCALE, new Random(RANDOM_SEED)), SAO_PAULO);

		List<OrderPlan> plans = planner.plan(scale, NOW);

		Map<OrderStatus, Long> activeByStatus = activeByStatus(plans);
		assertThat(plans).hasSize(scale.of(OrderSeedConstants.ORDER_BASE_COUNT));
		assertThat(activeByStatus).containsOnlyKeys(OrderStatus.RECEIVED, OrderStatus.PREPARING,
				OrderStatus.OUT_FOR_DELIVERY);
		assertThat(activeByStatus.values()).allSatisfy(
				count -> assertThat(count).isGreaterThanOrEqualTo(OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE));
		assertThat(activeByStatus.values().stream().mapToLong(Long::longValue).sum())
			.isLessThanOrEqualTo(OrderSeedConstants.MAX_ACTIVE_ORDERS);
	}

	@Test
	void smallScaleKeepsTwoOrdersOfEachActiveStatusPlusThoseAwaitingADecision() {
		OrderPlanner planner = new OrderPlanner(new Faker(SeedConstants.LOCALE, new Random(RANDOM_SEED)), SAO_PAULO);

		List<OrderPlan> plans = planner.plan(SeedScale.SMALL, NOW);

		assertThat(activeByStatus(plans)).containsOnly(
				Map.entry(OrderStatus.RECEIVED, (long) OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE),
				Map.entry(OrderStatus.PREPARING,
						(long) OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE
								+ OrderSeedConstants.PENDING_REQUESTS_BASE),
				Map.entry(OrderStatus.OUT_FOR_DELIVERY, (long) OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE));
		assertThat(plans).filteredOn(plan -> plan.cancellationRequest() == CancellationRequestStatus.PENDING)
			.hasSize(OrderSeedConstants.PENDING_REQUESTS_BASE)
			.allSatisfy(plan -> {
				assertThat(plan.outcome()).isEqualTo(OrderStatus.PREPARING);
				assertThat(plan.forFixedCustomer()).isFalse();
			});
	}

	@Test
	void fixedCustomerKeepsAReceivedAndAPreparingOrderWithoutRequest() {
		OrderPlanner planner = new OrderPlanner(new Faker(SeedConstants.LOCALE, new Random(RANDOM_SEED)), SAO_PAULO);

		List<OrderPlan> plans = planner.plan(SeedScale.SMALL, NOW);

		List<OrderPlan> fixedCustomerPlans = plans.stream().filter(OrderPlan::forFixedCustomer).toList();
		assertThat(fixedCustomerPlans).hasSize(OrderSeedConstants.FIXED_CUSTOMER_ORDER_COUNT)
			.allSatisfy(plan -> assertThat(plan.cancellationRequest()).isNull());
		assertThat(fixedCustomerPlans).extracting(OrderPlan::outcome)
			.filteredOn(OrderStatus::isActive)
			.containsExactlyInAnyOrder(OrderStatus.RECEIVED, OrderStatus.PREPARING);
	}

	private Map<OrderStatus, Long> activeByStatus(List<OrderPlan> plans) {
		return plans.stream()
			.filter(plan -> plan.outcome().isActive())
			.collect(Collectors.groupingBy(OrderPlan::outcome, Collectors.counting()));
	}

}
