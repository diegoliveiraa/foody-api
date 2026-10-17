package com.foody.api.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.catalog.dto.CartItem;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.repository.ProductRepository;
import com.foody.api.dashboard.DashboardConstants;
import com.foody.api.dashboard.dto.DashboardGranularity;
import com.foody.api.dashboard.dto.DashboardPeriod;
import com.foody.api.order.OrderConstants;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.CancellationRequest;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.shared.message.Messages;
import com.foody.api.shared.restaurant.RestaurantProperties;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.seed.catalog.CategoryFactory;
import com.foody.seed.catalog.ProductFactory;
import com.foody.seed.order.CancellationRequestFactory;
import com.foody.seed.order.OrderFactory;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import com.foody.testsupport.time.MutableClock;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@ApiIntegrationTest
class DashboardTest {

	private static final String PERIOD_PARAMETER = "period";

	private static final String SAO_PAULO = "America/Sao_Paulo";

	private static final Duration MINUTE = Duration.ofMinutes(1);

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserFactory users;

	@Autowired
	private OrderFactory orders;

	@Autowired
	private ProductFactory products;

	@Autowired
	private CategoryFactory categories;

	@Autowired
	private CancellationRequestFactory cancellationRequests;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private AccessTokens tokens;

	@Autowired
	private MutableClock clock;

	@Autowired
	private RestaurantProperties restaurant;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@AfterEach
	void resetClockAndStatistics() {
		clock.reset();
		statistics().setStatisticsEnabled(false);
	}

	@Test
	void dashboardCountsOrdersPlacedInThePeriod() {
		User staff = staff();
		User customer = users.create();
		Instant now = local("2041-03-20T15:00");
		orders.create(customer, local("2041-03-20T00:00"));
		orders.create(customer, local("2041-03-20T13:00"), delivered(staff));
		orders.create(customer, local("2041-03-20T14:59"));
		orders.create(customer, local("2041-03-19T23:30"),
				cancelled(customer, ActorSide.CUSTOMER, CancellationReason.CHANGED_MIND, "2041-03-19T23:40"));
		orders.create(customer, local("2041-03-14T00:00"));
		orders.create(customer, local("2041-03-13T23:59"));
		orders.create(customer, local("2041-02-19T00:00"));
		orders.create(customer, local("2041-02-18T23:59"));
		orders.create(customer, local("2041-03-20T15:30"));
		clock.fixAt(now);

		MvcTestResult today = overview(staff);
		MvcTestResult lastWeek = overview(staff, DashboardPeriod.LAST_7_DAYS);
		MvcTestResult lastMonth = overview(staff, DashboardPeriod.LAST_30_DAYS);

		assertThat(List.of(today, lastWeek, lastMonth)).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.OK);
			assertThat(result).bodyJson().extractingPath("$.timeZone").isEqualTo(SAO_PAULO);
			assertThat(result).bodyJson().extractingPath("$.generatedAt").isEqualTo(now.toString());
		});
		assertThat(today).bodyJson().extractingPath("$.period").isEqualTo(DashboardPeriod.TODAY.name());
		assertThat(today).bodyJson().extractingPath("$.orders.count").isEqualTo(3);
		assertThat(lastWeek).bodyJson().extractingPath("$.period").isEqualTo(DashboardPeriod.LAST_7_DAYS.name());
		assertThat(lastWeek).bodyJson().extractingPath("$.orders.count").isEqualTo(5);
		assertThat(lastMonth).bodyJson().extractingPath("$.period").isEqualTo(DashboardPeriod.LAST_30_DAYS.name());
		assertThat(lastMonth).bodyJson().extractingPath("$.orders.count").isEqualTo(7);
	}

	@Test
	void dashboardComparesWithTheSameSpanOfThePreviousPeriod() {
		User staff = staff();
		User customer = users.create();
		Stream
			.of("2042-06-10T09:00", "2042-06-10T14:00", "2042-06-09T00:00", "2042-06-09T08:00", "2042-06-09T14:59",
					"2042-06-09T15:00", "2042-06-09T22:00", "2042-05-28T00:00", "2042-06-03T14:59", "2042-06-03T15:00",
					"2042-05-27T23:59")
			.forEach(placedAt -> orders.create(customer, local(placedAt)));
		clock.fixAt(local("2042-06-10T15:00"));

		MvcTestResult today = overview(staff, DashboardPeriod.TODAY);
		MvcTestResult lastWeek = overview(staff, DashboardPeriod.LAST_7_DAYS);

		assertThat(today).bodyJson().extractingPath("$.orders.count").isEqualTo(2);
		assertThat(today).bodyJson().extractingPath("$.orders.previousCount").isEqualTo(3);
		assertThat(today).bodyJson().extractingPath("$.orders.changePercent").isEqualTo(-33.3);
		assertThat(lastWeek).bodyJson().extractingPath("$.orders.count").isEqualTo(7);
		assertThat(lastWeek).bodyJson().extractingPath("$.orders.previousCount").isEqualTo(2);
		assertThat(lastWeek).bodyJson().extractingPath("$.orders.changePercent").isEqualTo(250.0);
	}

	@Test
	void changePercentIsNullWithoutPreviousOrders() {
		User staff = staff();
		User customer = users.create();
		orders.create(customer, local("2043-04-15T08:00"));
		orders.create(customer, local("2043-04-15T11:00"));
		clock.fixAt(local("2043-04-15T12:00"));

		MvcTestResult today = overview(staff, DashboardPeriod.TODAY);
		MvcTestResult lastMonth = overview(staff, DashboardPeriod.LAST_30_DAYS);

		assertThat(List.of(today, lastMonth)).allSatisfy(result -> {
			assertThat(result).bodyJson().extractingPath("$.orders.count").isEqualTo(2);
			assertThat(result).bodyJson().extractingPath("$.orders.previousCount").isEqualTo(0);
			assertThat(result).bodyJson().extractingPath("$.orders.changePercent").isNull();
		});
	}

	@Test
	void revenueTicketAndTopProductsCountOnlyDeliveredOrders() {
		User staff = staff();
		User customer = users.create();
		Product burger = product("Smash", "10.00");
		Product fries = product("Batata frita", "10.01");
		Product lemonade = product("Limonada", "99.00");
		orders.create(customer, items(burger, 1), local("2044-07-08T12:00"), delivered(staff));
		orders.create(customer, items(fries, 1), local("2044-07-08T13:00"), delivered(staff));
		orders.create(customer, items(lemonade, 3), local("2044-07-08T14:00"), advancedTo(staff, OrderStatus.PREPARING)
			.andThen(cancelled(staff, ActorSide.RESTAURANT, CancellationReason.ITEM_UNAVAILABLE, "2044-07-08T14:20")));
		orders.create(customer, items(lemonade, 1), local("2044-07-08T15:00"), advancedTo(staff));
		orders.create(customer, items(burger, 2), local("2044-07-08T16:00"),
				advancedTo(staff, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY));
		orders.create(customer, items(lemonade, 5), local("2044-07-07T19:00"), delivered(staff));
		clock.fixAt(local("2044-07-08T21:00"));

		MvcTestResult today = overview(staff, DashboardPeriod.TODAY);
		MvcTestResult lastWeek = overview(staff, DashboardPeriod.LAST_7_DAYS);

		assertThat(today).bodyJson().extractingPath("$.orders.count").isEqualTo(5);
		assertThat(today).bodyJson().extractingPath("$.revenue").isEqualTo(20.01);
		assertThat(today).bodyJson().extractingPath("$.averageTicket").isEqualTo(10.0);
		assertThat(today).bodyJson()
			.extractingPath("$.topProducts[*].productId")
			.asArray()
			.containsExactly(idOf(fries), idOf(burger));
		assertThat(today).bodyJson()
			.extractingPath("$.topProducts[*].name")
			.asArray()
			.containsExactly("Batata frita", "Smash");
		assertThat(today).bodyJson().extractingPath("$.topProducts[*].quantity").asArray().containsExactly(1, 1);
		assertThat(today).bodyJson().extractingPath("$.topProducts[*].revenue").asArray().containsExactly(10.01, 10.0);
		assertThat(lastWeek).bodyJson().extractingPath("$.revenue").isEqualTo(515.01);
		assertThat(lastWeek).bodyJson().extractingPath("$.averageTicket").isEqualTo(171.67);
		assertThat(lastWeek).bodyJson()
			.extractingPath("$.topProducts[*].name")
			.asArray()
			.containsExactly("Limonada", "Batata frita", "Smash");
		assertThat(lastWeek).bodyJson().extractingPath("$.topProducts[0].quantity").isEqualTo(5);
		assertThat(lastWeek).bodyJson().extractingPath("$.topProducts[0].revenue").isEqualTo(495.0);
	}

	@Test
	void topProductsRankByRevenueAndKeepFive() {
		User staff = staff();
		User customer = users.create();
		Product alfa = product("Alfa", "10.00");
		Product bravo = product("Bravo", "15.00");
		Product charlie = product("Charlie novo", "25.00");
		Product delta = product("Delta", "10.00");
		Product echo = product("Echo", "10.00");
		Product foxtrot = product("Foxtrot", "10.00");
		orders.create(customer, List.of(item(alfa, 1), item(bravo, 2), item(delta, 2)), local("2045-09-02T09:00"),
				delivered(staff));
		orders.create(customer, List.of(item(alfa, 2), item(echo, 2), item(foxtrot, 1)), local("2045-09-02T11:00"),
				delivered(staff));
		orders.create(customer, items(charlie, 1), local("2045-09-02T19:00"), delivered(staff));
		rename(charlie, "Charlie antigo");
		orders.create(customer, items(charlie, 1), local("2045-09-02T10:00"), delivered(staff));
		clock.fixAt(local("2045-09-02T20:00"));

		MvcTestResult today = overview(staff, DashboardPeriod.TODAY);

		assertThat(today).bodyJson()
			.extractingPath("$.topProducts[*].name")
			.asArray()
			.containsExactly("Charlie novo", "Alfa", "Bravo", "Delta", "Echo");
		assertThat(today).bodyJson()
			.extractingPath("$.topProducts[*].productId")
			.asArray()
			.containsExactly(idOf(charlie), idOf(alfa), idOf(bravo), idOf(delta), idOf(echo));
		assertThat(today).bodyJson()
			.extractingPath("$.topProducts[*].quantity")
			.asArray()
			.containsExactly(2, 3, 2, 2, 2);
		assertThat(today).bodyJson()
			.extractingPath("$.topProducts[*].revenue")
			.asArray()
			.containsExactly(50.0, 30.0, 30.0, 20.0, 20.0);
	}

	@Test
	void ordersOverTimeFillsEmptyHourlyBucketsForToday() {
		User staff = staff();
		User customer = users.create();
		Stream
			.of("2046-01-12T00:10", "2046-01-12T09:15", "2046-01-12T09:45", "2046-01-12T14:30", "2046-01-11T23:59",
					"2046-01-12T14:40")
			.forEach(placedAt -> orders.create(customer, local(placedAt)));
		clock.fixAt(local("2046-01-12T14:35"));

		MvcTestResult today = overview(staff, DashboardPeriod.TODAY);

		assertThat(today).bodyJson()
			.extractingPath("$.ordersOverTime.granularity")
			.isEqualTo(DashboardGranularity.HOUR.name());
		assertThat(today).bodyJson()
			.extractingPath("$.ordersOverTime.points[*].bucketStart")
			.asArray()
			.containsExactlyElementsOf(IntStream.rangeClosed(0, 14)
				.mapToObj(hour -> local("2046-01-12T%02d:00".formatted(hour)).toString())
				.toList());
		assertThat(today).bodyJson()
			.extractingPath("$.ordersOverTime.points[*].count")
			.asArray()
			.containsExactly(1, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 1);
	}

	@Test
	void ordersOverTimeHasOneBucketPerLocalDay() {
		User staff = staff();
		User customer = users.create();
		Stream.of("2047-08-14T00:00", "2047-08-14T23:59", "2047-08-17T12:00", "2047-08-20T09:00", "2047-08-13T23:59")
			.forEach(placedAt -> orders.create(customer, local(placedAt)));
		clock.fixAt(local("2047-08-20T10:00"));

		MvcTestResult lastWeek = overview(staff, DashboardPeriod.LAST_7_DAYS);
		MvcTestResult lastMonth = overview(staff, DashboardPeriod.LAST_30_DAYS);

		assertThat(lastWeek).bodyJson()
			.extractingPath("$.ordersOverTime.granularity")
			.isEqualTo(DashboardGranularity.DAY.name());
		assertThat(lastWeek).bodyJson()
			.extractingPath("$.ordersOverTime.points[*].bucketStart")
			.asArray()
			.containsExactlyElementsOf(IntStream.rangeClosed(14, 20)
				.mapToObj(day -> local("2047-08-%02dT00:00".formatted(day)).toString())
				.toList());
		assertThat(lastWeek).bodyJson()
			.extractingPath("$.ordersOverTime.points[*].count")
			.asArray()
			.containsExactly(2, 0, 0, 1, 0, 0, 1);
		assertThat(lastMonth).bodyJson()
			.extractingPath("$.ordersOverTime.points")
			.asArray()
			.hasSize(DashboardPeriod.LAST_30_DAYS.days());
		assertThat(lastMonth).bodyJson()
			.extractingPath("$.ordersOverTime.points[0].bucketStart")
			.isEqualTo(local("2047-07-22T00:00").toString());
		assertThat(lastMonth).bodyJson()
			.extractingPath("$.ordersOverTime.points[22].bucketStart")
			.isEqualTo(local("2047-08-13T00:00").toString());
		assertThat(lastMonth).bodyJson()
			.extractingPath("$.ordersOverTime.points[*].count")
			.asArray()
			.containsExactlyElementsOf(lastMonthCounts());
	}

	@Test
	void averageStageMinutesComeFromTransitionsInThePeriod() {
		User staff = staff();
		User customer = users.create();
		orders.create(customer, local("2048-05-05T10:00"),
				advancedAt(staff, "2048-05-05T10:03", "2048-05-05T10:20", "2048-05-05T10:45"));
		orders.create(customer, local("2048-05-05T11:00"),
				advancedAt(staff, "2048-05-05T11:04", "2048-05-05T11:22", "2048-05-05T11:44"));
		orders.create(customer, local("2048-05-04T23:50"),
				advancedAt(staff, "2048-05-05T00:04", "2048-05-05T00:19", "2048-05-05T00:49"));
		orders.create(customer, local("2048-05-04T10:00"),
				advancedAt(staff, "2048-05-04T10:30", "2048-05-04T11:00", "2048-05-04T12:00"));
		orders.create(customer, local("2048-05-05T12:00"), advancedAt(staff, "2048-05-05T12:05")
			.andThen(cancelled(staff, ActorSide.RESTAURANT, CancellationReason.ITEM_UNAVAILABLE, "2048-05-05T12:30")));
		orders.create(customer, local("2048-05-04T23:00"),
				advancedAt(staff, "2048-05-04T23:40", "2048-05-05T00:10", "2048-05-05T00:30"));
		clock.fixAt(local("2048-05-05T18:00"));

		MvcTestResult today = overview(staff, DashboardPeriod.TODAY);

		assertThat(today).bodyJson().extractingPath("$.averageStageMinutes.receivedToPreparing").isEqualTo(7);
		assertThat(today).bodyJson().extractingPath("$.averageStageMinutes.preparingToOutForDelivery").isEqualTo(20);
		assertThat(today).bodyJson().extractingPath("$.averageStageMinutes.outForDeliveryToDelivered").isEqualTo(24);
	}

	@Test
	void cancellationsSplitByReasonAndSide() {
		User staff = staff();
		User customer = users.create();
		IntStream.range(0, 7)
			.forEach(index -> orders.create(customer, local("2049-10-10T10:00").plus(MINUTE.multipliedBy(index))));
		orders.create(customer, local("2049-10-10T11:00"),
				cancelled(customer, ActorSide.CUSTOMER, CancellationReason.TAKING_TOO_LONG, "2049-10-10T11:20"));
		orders.create(customer, local("2049-10-10T12:00"),
				advancedTo(staff, OrderStatus.PREPARING).andThen(approvedRequest(staff, "2049-10-10T12:30")));
		orders.create(customer, local("2049-10-10T13:00"),
				cancelled(staff, ActorSide.RESTAURANT, CancellationReason.ITEM_UNAVAILABLE, "2049-10-10T13:05"));
		orders.create(customer, local("2049-10-10T14:00"), cancelled(customer, ActorSide.CUSTOMER,
				CancellationReason.OTHER, "Endereço errado", "2049-10-10T14:03"));
		orders.create(customer, local("2049-10-10T15:00"),
				cancelled(staff, ActorSide.RESTAURANT, CancellationReason.OTHER, "Fora da área", "2049-10-10T15:02"));
		orders.create(customer, local("2049-10-09T23:50"),
				cancelled(staff, ActorSide.RESTAURANT, CancellationReason.STORE_CLOSED, "2049-10-10T00:05"));
		clock.fixAt(local("2049-10-10T20:00"));

		MvcTestResult today = overview(staff, DashboardPeriod.TODAY);

		assertThat(today).bodyJson().extractingPath("$.orders.count").isEqualTo(12);
		assertThat(today).bodyJson().extractingPath("$.cancellations.count").isEqualTo(5);
		assertThat(today).bodyJson().extractingPath("$.cancellations.ratePercent").isEqualTo(41.7);
		assertThat(today).bodyJson()
			.extractingPath("$.cancellations.byReason[*].reason")
			.asArray()
			.containsExactly(CancellationReason.TAKING_TOO_LONG.name(), CancellationReason.ITEM_UNAVAILABLE.name(),
					CancellationReason.OTHER.name(), CancellationReason.OTHER.name());
		assertThat(today).bodyJson()
			.extractingPath("$.cancellations.byReason[*].cancelledBy")
			.asArray()
			.containsExactly(ActorSide.CUSTOMER.name(), ActorSide.RESTAURANT.name(), ActorSide.CUSTOMER.name(),
					ActorSide.RESTAURANT.name());
		assertThat(today).bodyJson()
			.extractingPath("$.cancellations.byReason[*].count")
			.asArray()
			.containsExactly(2, 1, 1, 1);
	}

	@Test
	void activeOrdersAndPendingRequestsIgnoreThePeriod() throws UnsupportedEncodingException {
		User staff = staff();
		User customer = users.create();
		clock.fixAt(local("2050-03-03T12:00"));
		MvcTestResult before = overview(staff, DashboardPeriod.TODAY);
		orders.create(customer, local("1995-06-01T10:00"));
		orders.create(customer, local("1995-06-01T11:00"));
		orders.create(customer, local("1995-06-02T10:00"), advancedTo(staff, OrderStatus.PREPARING));
		Stream.of("1995-06-03T10:00", "1995-06-03T11:00", "1995-06-03T12:00")
			.forEach(placedAt -> orders.create(customer, local(placedAt),
					advancedTo(staff, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY)));
		orders.create(customer, local("1995-06-04T10:00"), delivered(staff));
		orders.create(customer, local("1995-06-04T11:00"),
				cancelled(customer, ActorSide.CUSTOMER, CancellationReason.CHANGED_MIND, "1995-06-04T11:05"));
		cancellationRequests.create(
				orders.create(customer, local("1995-05-01T09:00"), advancedTo(staff, OrderStatus.PREPARING)),
				request -> request.decline(staff, null, request.getRequestedAt().plus(MINUTE)));
		List<CancellationRequest> pending = IntStream.range(0, 11)
			.mapToObj(hour -> orders.create(customer, local("1995-05-01T10:00").plus(Duration.ofHours(hour)),
					advancedTo(staff, OrderStatus.PREPARING)))
			.map(cancellationRequests::create)
			.toList();

		MvcTestResult today = overview(staff, DashboardPeriod.TODAY);
		MvcTestResult lastMonth = overview(staff, DashboardPeriod.LAST_30_DAYS);

		assertThat(numberAt(today, "$.activeByStatus.received") - numberAt(before, "$.activeByStatus.received"))
			.isEqualTo(2);
		assertThat(numberAt(today, "$.activeByStatus.preparing") - numberAt(before, "$.activeByStatus.preparing"))
			.isEqualTo(13);
		assertThat(numberAt(today, "$.activeByStatus.outForDelivery")
				- numberAt(before, "$.activeByStatus.outForDelivery"))
			.isEqualTo(3);
		assertThat(numberAt(today, "$.pendingCancellationRequests.count")
				- numberAt(before, "$.pendingCancellationRequests.count"))
			.isEqualTo(11);
		assertThat(today).bodyJson()
			.extractingPath("$.pendingCancellationRequests.items[*].orderId")
			.asArray()
			.containsExactlyElementsOf(pending.subList(0, 10).stream().map(this::orderIdOf).toList());
		assertThat(today).bodyJson()
			.extractingPath("$.pendingCancellationRequests.items[*].number")
			.asArray()
			.containsExactlyElementsOf(pending.subList(0, 10).stream().map(this::orderNumberOf).toList());
		assertThat(today).bodyJson()
			.extractingPath("$.pendingCancellationRequests.items[0].requestedAt")
			.isEqualTo(pending.getFirst().getRequestedAt().toString());
		assertThat(lastMonth).bodyJson()
			.extractingPath("$.activeByStatus")
			.isEqualTo(JsonPath.read(today.getResponse().getContentAsString(), "$.activeByStatus"));
		assertThat(lastMonth).bodyJson()
			.extractingPath("$.pendingCancellationRequests")
			.isEqualTo(JsonPath.read(today.getResponse().getContentAsString(), "$.pendingCancellationRequests"));
	}

	@Test
	void emptyPeriodReturnsZerosAndNulls() {
		User staff = staff();
		clock.fixAt(local("2051-11-11T09:30"));

		MvcTestResult today = overview(staff, DashboardPeriod.TODAY);
		MvcTestResult lastMonth = overview(staff, DashboardPeriod.LAST_30_DAYS);

		assertThat(List.of(today, lastMonth)).allSatisfy(result -> {
			assertThat(result).hasStatus(HttpStatus.OK);
			assertThat(result).bodyJson().extractingPath("$.orders.count").isEqualTo(0);
			assertThat(result).bodyJson().extractingPath("$.orders.previousCount").isEqualTo(0);
			assertThat(result).bodyJson().extractingPath("$.orders.changePercent").isNull();
			assertThat(result).bodyJson().extractingPath("$.revenue").isEqualTo(0.0);
			assertThat(result).bodyJson().extractingPath("$.averageTicket").isNull();
			assertThat(result).bodyJson().extractingPath("$.ordersOverTime.points[*].count").asArray().containsOnly(0);
			assertThat(result).bodyJson().extractingPath("$.topProducts").asArray().isEmpty();
			assertThat(result).bodyJson().extractingPath("$.averageStageMinutes.receivedToPreparing").isNull();
			assertThat(result).bodyJson().extractingPath("$.averageStageMinutes.preparingToOutForDelivery").isNull();
			assertThat(result).bodyJson().extractingPath("$.averageStageMinutes.outForDeliveryToDelivered").isNull();
			assertThat(result).bodyJson().extractingPath("$.cancellations.count").isEqualTo(0);
			assertThat(result).bodyJson().extractingPath("$.cancellations.ratePercent").isNull();
			assertThat(result).bodyJson().extractingPath("$.cancellations.byReason").asArray().isEmpty();
		});
		assertThat(today).bodyJson().extractingPath("$.ordersOverTime.points").asArray().hasSize(10);
		assertThat(lastMonth).bodyJson()
			.extractingPath("$.ordersOverTime.points")
			.asArray()
			.hasSize(DashboardPeriod.LAST_30_DAYS.days());
	}

	@Test
	void dashboardRunsAFixedNumberOfQueries() {
		User staff = staff();
		User customer = users.create();
		Instant quietNow = local("2052-02-02T12:00");
		Instant busyNow = local("2053-02-02T20:00");
		Product burger = product("Smash", "32.90");
		Product pizza = product("Margherita", "49.90");
		Product lemonade = product("Limonada", "12.00");
		orders.create(customer, items(burger, 1), local("2052-02-02T10:00"), delivered(staff));
		IntStream.range(0, 6)
			.forEach(day -> orders.create(customer, List.of(item(burger, 2), item(pizza, 1), item(lemonade, 3)),
					local("2053-01-27T12:00").plus(Duration.ofDays(day)), delivered(staff)));
		orders.create(customer, local("2053-02-01T12:00"),
				cancelled(customer, ActorSide.CUSTOMER, CancellationReason.CHANGED_MIND, "2053-02-01T12:05"));
		orders.create(customer, local("2053-02-01T13:00"),
				cancelled(staff, ActorSide.RESTAURANT, CancellationReason.STORE_CLOSED, "2053-02-01T13:05"));
		orders.create(customer, local("2053-02-02T13:00"), advancedTo(staff, OrderStatus.PREPARING)
			.andThen(cancelled(staff, ActorSide.RESTAURANT, CancellationReason.ITEM_UNAVAILABLE, "2053-02-02T13:30")));
		orders.create(customer, local("2053-02-02T18:00"));
		orders.create(customer, local("2053-02-02T18:30"), advancedTo(staff, OrderStatus.PREPARING));
		orders.create(customer, local("2053-02-02T19:00"),
				advancedTo(staff, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY));
		Stream.of("2053-02-02T19:10", "2053-02-02T19:20")
			.forEach(placedAt -> cancellationRequests
				.create(orders.create(customer, local(placedAt), advancedTo(staff, OrderStatus.PREPARING))));
		statistics().setStatisticsEnabled(true);

		CountedResult quiet = overviewCountingStatements(staff, quietNow);
		CountedResult busy = overviewCountingStatements(staff, busyNow);

		assertThat(quiet.result()).bodyJson().extractingPath("$.orders.count").isEqualTo(1);
		assertThat(busy.result()).bodyJson().extractingPath("$.orders.count").isEqualTo(14);
		assertThat(busy.result()).bodyJson().extractingPath("$.topProducts").asArray().hasSize(3);
		assertThat(busy.result()).bodyJson().extractingPath("$.cancellations.byReason").asArray().hasSize(3);
		assertThat(busy.statements()).isPositive().isEqualTo(quiet.statements());
	}

	@Test
	void customerCannotReadTheDashboard() {
		User customer = users.create();

		MvcTestResult result = overview(customer, DashboardPeriod.TODAY);

		assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.FORBIDDEN.name());
	}

	@Test
	void anonymousCannotReadTheDashboard() {
		String path = DashboardConstants.DASHBOARD_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.UNAUTHENTICATED.name());
	}

	@Test
	void unknownPeriodIsRejected() {
		User staff = staff();

		MvcTestResult result = mvc.get()
			.uri(DashboardConstants.DASHBOARD_PATH)
			.param(PERIOD_PARAMETER, "YESTERDAY")
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(staff))
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_PARAMETER.name());
	}

	private CountedResult overviewCountingStatements(User staff, Instant now) {
		clock.fixAt(now);
		Statistics statistics = statistics();
		statistics.clear();
		MvcTestResult result = overview(staff, DashboardPeriod.LAST_7_DAYS);
		return new CountedResult(result, statistics.getPrepareStatementCount());
	}

	private MvcTestResult overview(User caller) {
		return mvc.get()
			.uri(DashboardConstants.DASHBOARD_PATH)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.exchange();
	}

	private MvcTestResult overview(User caller, DashboardPeriod period) {
		return mvc.get()
			.uri(DashboardConstants.DASHBOARD_PATH)
			.param(PERIOD_PARAMETER, period.name())
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(caller))
			.exchange();
	}

	private Consumer<Order> delivered(User staff) {
		return advancedTo(staff, OrderStatus.PREPARING, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED);
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

	private Consumer<Order> advancedAt(User staff, String... localDateTimes) {
		return order -> Arrays.stream(localDateTimes)
			.forEach(occurredAt -> order.advance(OrderConstants.NEXT_STATUS.get(order.getStatus()), staff,
					ActorSide.RESTAURANT, local(occurredAt)));
	}

	private Consumer<Order> cancelled(User actor, ActorSide side, CancellationReason reason, String localDateTime) {
		return cancelled(actor, side, reason, null, localDateTime);
	}

	private Consumer<Order> cancelled(User actor, ActorSide side, CancellationReason reason, @Nullable String note,
			String localDateTime) {
		return order -> order.cancel(actor, side, reason, note, local(localDateTime));
	}

	private Consumer<Order> approvedRequest(User staff, String localDateTime) {
		return order -> order.cancel(staff, ActorSide.RESTAURANT, ActorSide.CUSTOMER,
				CancellationReason.TAKING_TOO_LONG, null, local(localDateTime));
	}

	private Product product(String name, String price) {
		return products.create(categories.create(), name, new BigDecimal(price));
	}

	private void rename(Product product, String name) {
		product.update(product.getCategory(), name, product.getDescription(), product.getPrice());
		productRepository.save(product);
	}

	private List<CartItem> items(Product product, int quantity) {
		return List.of(item(product, quantity));
	}

	private CartItem item(Product product, int quantity) {
		return new CartItem(product.getId(), quantity);
	}

	private List<Integer> lastMonthCounts() {
		List<Integer> counts = new ArrayList<>(Collections.nCopies(DashboardPeriod.LAST_30_DAYS.days(), 0));
		counts.set(22, 1);
		counts.set(23, 2);
		counts.set(26, 1);
		counts.set(29, 1);
		return counts;
	}

	private long numberAt(MvcTestResult result, String path) throws UnsupportedEncodingException {
		Number value = JsonPath.read(result.getResponse().getContentAsString(), path);
		return value.longValue();
	}

	private Instant local(String localDateTime) {
		return LocalDateTime.parse(localDateTime).atZone(restaurant.timeZone()).toInstant();
	}

	private User staff() {
		return users.create(user -> user.changeRole(Role.STAFF));
	}

	private String idOf(Product product) {
		return product.getId().toString();
	}

	private String orderIdOf(CancellationRequest request) {
		return request.getOrder().getId().toString();
	}

	private Integer orderNumberOf(CancellationRequest request) {
		return Math.toIntExact(request.getOrder().getNumber());
	}

	private Statistics statistics() {
		return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
	}

	record CountedResult(MvcTestResult result, long statements) {
	}

}
