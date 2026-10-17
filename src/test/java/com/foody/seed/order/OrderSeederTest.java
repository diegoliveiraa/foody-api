package com.foody.seed.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.auth.repository.RefreshTokenRepository;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.repository.CategoryRepository;
import com.foody.api.catalog.repository.ProductRepository;
import com.foody.api.order.OrderConstants;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.BrazilianState;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.CancellationRequest;
import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderItem;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.entity.OrderStatusHistory;
import com.foody.api.order.repository.CancellationRequestRepository;
import com.foody.api.order.repository.OrderRepository;
import com.foody.api.shared.restaurant.RestaurantProperties;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.SeedConstants;
import com.foody.seed.SeedProperties;
import com.foody.seed.SeedRunner;
import com.foody.seed.SeedScale;
import com.foody.seed.catalog.CatalogSeeder;
import com.foody.seed.catalog.CategoryFactory;
import com.foody.seed.catalog.ProductFactory;
import com.foody.seed.user.IdentitySeedProperties;
import com.foody.seed.user.IdentitySeeder;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AccessTokens;
import com.foody.testsupport.time.MutableClock;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.datafaker.Faker;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.support.TransactionOperations;

@ApiIntegrationTest
class OrderSeederTest {

	private static final Instant NOW = Instant.parse("2026-10-17T22:30:00Z");

	private static final long RANDOM_SEED = 42;

	private static final IdentitySeedProperties IDENTITY = new IdentitySeedProperties(
			new IdentitySeedProperties.Admin("admin.orders@foody.test", "admin-orders-pass-1"),
			new IdentitySeedProperties.Account("staff.orders@foody.test"),
			new IdentitySeedProperties.Account("customer.orders@foody.test"), "default-orders-pass-1");

	private static final LocalTime LUNCH_START = LocalTime.of(11, 0);

	private static final LocalTime LUNCH_END = LocalTime.of(14, 0);

	private static final LocalTime DINNER_START = LocalTime.of(18, 0);

	private static final LocalTime DINNER_END = LocalTime.of(22, 0);

	private static final int PEAK_PERCENT_FLOOR = 80;

	private static final int CANCELLED_PERCENT_FLOOR = 5;

	private static final int CANCELLED_PERCENT_CEILING = 30;

	private static final int SMALL_QUANTITY_PERCENT_FLOOR = 75;

	private static final int MAX_ITEMS = 4;

	private static final int MAX_QUANTITY = 3;

	private static final int SMALL_QUANTITY = 2;

	private static final int MIN_DISTINCT_DAYS = 25;

	private static final int BOARD_PAGE_SIZE = 100;

	private static final int MIN_PENDING_REQUESTS = 2;

	private static final int MAX_PENDING_REQUESTS = 3;

	private static final Duration DECISION_WINDOW = Duration.ofMinutes(5);

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private AccessTokens tokens;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private CancellationRequestRepository cancellationRequestRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private UserFactory users;

	@Autowired
	private CategoryFactory categories;

	@Autowired
	private ProductFactory products;

	@Autowired
	private OrderFactory orders;

	@Autowired
	private Faker faker;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MutableClock clock;

	@Autowired
	private RestaurantProperties restaurant;

	@Autowired
	private TransactionOperations transactions;

	private OrderSeeder seeder;

	@BeforeEach
	void startFromSeededIdentityAndCatalog() {
		orderRepository.deleteAllInBatch();
		refreshTokenRepository.deleteAllInBatch();
		productRepository.deleteAllInBatch();
		categoryRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
		clock.fixAt(NOW);
		new IdentitySeeder(userRepository, users, faker, passwordEncoder, IDENTITY).seed(SeedScale.SMALL);
		new CatalogSeeder(categoryRepository, productRepository, categories, products, faker).seed(SeedScale.SMALL);
		Faker seededFaker = new Faker(SeedConstants.LOCALE, new Random(RANDOM_SEED));
		seeder = new OrderSeeder(orderRepository, cancellationRequestRepository, userRepository, productRepository,
				orders, seededFaker, clock, restaurant, IDENTITY);
	}

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void orderSeederCreatesScaledOrdersAcrossThirtyDays() {
		SeedScale scale = SeedScale.MEDIUM;

		seed(scale);

		List<SeededOrder> seeded = seededOrders();
		List<SeededOrder> older = seeded.stream()
			.filter(order -> order.placedAt().isBefore(recentWindowStart()))
			.toList();
		Map<UUID, BigDecimal> menuPrices = productRepository.findAllOnMenu()
			.stream()
			.collect(Collectors.toMap(Product::getId, Product::getPrice));
		List<SeededItem> items = seeded.stream().flatMap(order -> order.items().stream()).toList();
		assertThat(seeded).hasSize(scale.of(OrderSeedConstants.ORDER_BASE_COUNT));
		assertThat(seeded).extracting(SeededOrder::placedAt)
			.allSatisfy(placedAt -> assertThat(placedAt)
				.isBetween(NOW.minus(Duration.ofDays(OrderSeedConstants.HISTORY_DAYS)), NOW));
		assertThat(seeded).extracting(SeededOrder::number).isSorted().doesNotHaveDuplicates();
		assertThat(seeded.stream().map(order -> restaurant.dayOf(order.placedAt())).distinct().count())
			.isGreaterThanOrEqualTo(MIN_DISTINCT_DAYS);
		assertThat(percentOf(older.stream().filter(this::placedAtPeak).count(), older.size()))
			.isGreaterThanOrEqualTo(PEAK_PERCENT_FLOOR);
		assertThat(older).extracting(SeededOrder::status).containsOnly(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
		assertThat(percentOf(older.stream().filter(order -> order.status() == OrderStatus.CANCELLED).count(),
				older.size()))
			.isBetween(CANCELLED_PERCENT_FLOOR, CANCELLED_PERCENT_CEILING);
		assertThat(seeded).allSatisfy(order -> {
			assertThat(order.items()).hasSizeBetween(1, MAX_ITEMS);
			assertThat(order.items()).extracting(SeededItem::productId).doesNotHaveDuplicates();
			assertThat(order.total()).isEqualByComparingTo(order.itemsTotal());
			assertThat(order.customerRole()).isEqualTo(Role.CUSTOMER);
			assertThat(order.customerActive()).isTrue();
			assertThat(order.zipCode()).matches(OrderConstants.ZIP_CODE_PATTERN.pattern());
			assertThat(order.state()).isEqualTo(BrazilianState.SP);
		});
		assertThat(items).allSatisfy(item -> {
			assertThat(item.quantity()).isBetween(1, MAX_QUANTITY);
			assertThat(menuPrices).containsKey(item.productId());
			assertThat(item.unitPrice()).isEqualByComparingTo(menuPrices.get(item.productId()));
		});
		assertThat(percentOf(items.stream().filter(item -> item.quantity() <= SMALL_QUANTITY).count(), items.size()))
			.isGreaterThanOrEqualTo(SMALL_QUANTITY_PERCENT_FLOOR);
	}

	@Test
	void orderSeederKeepsActiveOrdersRecent() {
		SeedScale scale = SeedScale.SMALL;

		seed(scale);

		List<SeededOrder> seeded = seededOrders();
		List<SeededOrder> active = seeded.stream().filter(order -> order.status().isActive()).toList();
		Map<OrderStatus, Long> activeByStatus = active.stream()
			.collect(Collectors.groupingBy(SeededOrder::status, Collectors.counting()));
		assertThat(active).extracting(SeededOrder::placedAt)
			.allSatisfy(placedAt -> assertThat(placedAt).isBetween(recentWindowStart(), NOW));
		assertThat(activeByStatus).containsOnlyKeys(OrderStatus.RECEIVED, OrderStatus.PREPARING,
				OrderStatus.OUT_FOR_DELIVERY);
		assertThat(activeByStatus.values()).allSatisfy(count -> assertThat(count)
			.isGreaterThanOrEqualTo((long) scale.of(OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE)));
		assertThat(seeded).filteredOn(order -> order.placedAt().isBefore(recentWindowStart()))
			.extracting(SeededOrder::status)
			.containsOnly(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
		assertThat(seeded).filteredOn(order -> order.status() == OrderStatus.DELIVERED)
			.filteredOn(order -> restaurant.dayOf(order.placedAt()).equals(restaurant.dayOf(NOW)))
			.isNotEmpty();
	}

	@Test
	void orderSeederCapsActiveOrdersSoTheBoardFitsOnePage() {
		SeedScale scale = SeedScale.MEDIUM;
		seed(scale);
		User staff = userRepository.findByEmail(IDENTITY.staff().email()).orElseThrow();

		MvcTestResult board = mvc.get()
			.uri(OrderConstants.ORDERS_PATH)
			.param("status", OrderStatus.RECEIVED.name(), OrderStatus.PREPARING.name(),
					OrderStatus.OUT_FOR_DELIVERY.name())
			.param("pageSize", String.valueOf(BOARD_PAGE_SIZE))
			.param("sort", "placedAt,asc")
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(staff))
			.exchange();

		assertThat(orderRepository.count()).isEqualTo(scale.of(OrderSeedConstants.ORDER_BASE_COUNT));
		assertThat(board).hasStatus(HttpStatus.OK);
		assertThat(board).bodyJson()
			.extractingPath("$.meta.totalItems")
			.asNumber()
			.satisfies(total -> assertThat(total.intValue()).isBetween(
					OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE * OrderSeedConstants.ACTIVE_STATUSES.size(),
					OrderSeedConstants.MAX_ACTIVE_ORDERS));
		assertThat(board).bodyJson().extractingPath("$.meta.hasNext").isEqualTo(false);
		assertThat(board).bodyJson().extractingPath("$.meta.pageSize").isEqualTo(BOARD_PAGE_SIZE);
	}

	@Test
	void orderSeederGivesTheFixedCustomerAnActiveOrder() {
		SeedScale scale = SeedScale.SMALL;

		seed(scale);

		List<SeededOrder> fixedCustomerOrders = seededOrders().stream()
			.filter(order -> order.customerEmail().equals(IDENTITY.customer().email()))
			.toList();
		assertThat(fixedCustomerOrders).hasSize(OrderSeedConstants.FIXED_CUSTOMER_ORDER_COUNT);
		assertThat(fixedCustomerOrders).anyMatch(order -> order.status().isActive());
		assertThat(fixedCustomerOrders).anyMatch(order -> !order.status().isActive());
	}

	@Test
	void orderSeederHistoriesAreChronological() {
		SeedScale scale = SeedScale.SMALL;

		seed(scale);

		List<SeededOrder> seeded = seededOrders();
		assertThat(seeded).allSatisfy(order -> {
			List<SeededStep> history = order.history();
			SeededStep first = history.getFirst();
			assertThat(first.fromStatus()).isNull();
			assertThat(first.toStatus()).isEqualTo(OrderStatus.RECEIVED);
			assertThat(first.occurredAt()).isEqualTo(order.placedAt());
			assertThat(history).extracting(SeededStep::occurredAt)
				.isSorted()
				.doesNotHaveDuplicates()
				.allSatisfy(occurredAt -> assertThat(occurredAt).isBeforeOrEqualTo(NOW));
			assertThat(history.subList(1, history.size())).extracting(SeededStep::fromStatus)
				.containsExactlyElementsOf(
						history.subList(0, history.size() - 1).stream().map(SeededStep::toStatus).toList());
			assertThat(history.getLast().toStatus()).isEqualTo(order.status());
			assertThat(history).filteredOn(step -> step.actorSide() == ActorSide.CUSTOMER)
				.extracting(SeededStep::actorId)
				.containsOnly(order.customerId());
			assertThat(history).filteredOn(step -> step.actorSide() == ActorSide.RESTAURANT)
				.extracting(SeededStep::actorRole)
				.allMatch(role -> role == Role.STAFF);
		});
		assertThat(seeded).filteredOn(order -> order.status() == OrderStatus.CANCELLED)
			.filteredOn(order -> order.request() == null)
			.isNotEmpty()
			.allSatisfy(order -> {
				SeededStep cancellation = order.history().getLast();
				assertThat(order.cancelledBy()).isEqualTo(cancellation.actorSide());
				assertThat(order.cancelReason()).isEqualTo(cancellation.reason());
				assertThat(order.cancelNote()).isEqualTo(cancellation.note());
				assertThat(cancellation.reason()).isNotNull()
					.satisfies(reason -> assertThat(reason.allows(cancellation.actorSide())).isTrue());
			});
		assertThat(seeded).filteredOn(order -> order.cancelReason() == CancellationReason.OTHER)
			.allSatisfy(order -> assertThat(order.cancelNote()).isNotBlank());
		assertThat(seeded).filteredOn(order -> order.cancelReason() == CancellationReason.DELIVERY_FAILED)
			.allSatisfy(order -> assertThat(order.history().getLast().fromStatus())
				.isEqualTo(OrderStatus.OUT_FOR_DELIVERY));
		assertThat(seeded).filteredOn(order -> order.cancelledBy() == ActorSide.CUSTOMER)
			.filteredOn(order -> order.request() == null)
			.allSatisfy(order -> assertThat(order.history().getLast().fromStatus()).isEqualTo(OrderStatus.RECEIVED));
		assertThat(seeded).filteredOn(order -> order.status() != OrderStatus.CANCELLED)
			.allSatisfy(order -> assertThat(order.cancelledBy()).isNull());
	}

	@Test
	void orderSeederCreatesPendingRequestsOnPreparingOrders() {
		seed(SeedScale.SMALL);
		User staff = userRepository.findByEmail(IDENTITY.staff().email()).orElseThrow();
		List<SeededOrder> pending = withRequest(seededOrders(), CancellationRequestStatus.PENDING);

		MvcTestResult board = mvc.get()
			.uri(OrderConstants.ORDERS_PATH)
			.param("status", OrderStatus.PREPARING.name())
			.param("pageSize", String.valueOf(BOARD_PAGE_SIZE))
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(staff))
			.exchange();

		assertThat(pending).hasSizeBetween(MIN_PENDING_REQUESTS, MAX_PENDING_REQUESTS).allSatisfy(order -> {
			SeededRequest request = order.request();
			assertThat(order.status()).isEqualTo(OrderStatus.PREPARING);
			assertThat(order.customerEmail()).isNotEqualTo(IDENTITY.customer().email());
			assertThat(order.placedAt()).isBetween(recentWindowStart(), NOW);
			assertThat(request.requestedAt()).isAfter(stepTo(order, OrderStatus.PREPARING).occurredAt())
				.isBeforeOrEqualTo(NOW);
			assertThat(request.reason().allows(ActorSide.CUSTOMER)).isTrue();
			assertThat(request.decidedAt()).isNull();
			assertThat(request.decidedById()).isNull();
			assertThat(order.history().getLast().toStatus()).isEqualTo(OrderStatus.PREPARING);
		});
		assertThat(pending).filteredOn(order -> order.request().reason() == CancellationReason.OTHER)
			.allSatisfy(order -> assertThat(order.request().note()).isNotBlank());
		assertThat(board).hasStatus(HttpStatus.OK);
		assertThat(board).bodyJson()
			.extractingPath("$.data[?(@.hasPendingCancellationRequest == true)].number")
			.asArray()
			.containsExactlyInAnyOrderElementsOf(pending.stream().map(order -> (int) order.number()).toList());
	}

	@Test
	void orderSeederKeepsTheFixedCustomersActiveOrderRequestable() {
		seed(SeedScale.SMALL);
		User fixedCustomer = userRepository.findByEmail(IDENTITY.customer().email()).orElseThrow();
		List<SeededOrder> activeOrders = seededOrders().stream()
			.filter(order -> order.customerEmail().equals(IDENTITY.customer().email()))
			.filter(order -> order.status().isActive())
			.toList();
		SeededOrder preparing = withStatus(activeOrders, OrderStatus.PREPARING);
		SeededOrder received = withStatus(activeOrders, OrderStatus.RECEIVED);

		MvcTestResult preparingDetail = myOrder(fixedCustomer, preparing.id());
		MvcTestResult receivedDetail = myOrder(fixedCustomer, received.id());

		assertThat(activeOrders).extracting(SeededOrder::status)
			.containsExactlyInAnyOrder(OrderStatus.RECEIVED, OrderStatus.PREPARING);
		assertThat(preparing.request()).isNull();
		assertThat(preparingDetail).hasStatus(HttpStatus.OK);
		assertThat(preparingDetail).bodyJson().extractingPath("$.canRequestCancellation").isEqualTo(true);
		assertThat(preparingDetail).bodyJson().extractingPath("$.cancellationRequest").isNull();
		assertThat(preparingDetail).bodyJson().extractingPath("$.cancellationReasons").asArray().isNotEmpty();
		assertThat(receivedDetail).hasStatus(HttpStatus.OK);
		assertThat(receivedDetail).bodyJson().extractingPath("$.canCancel").isEqualTo(true);
	}

	@Test
	void orderSeederDecidedRequestsAreCoherent() {
		SeedScale scale = SeedScale.SMALL;

		seed(scale);

		List<SeededOrder> seeded = seededOrders();
		List<SeededOrder> approved = withRequest(seeded, CancellationRequestStatus.APPROVED);
		List<SeededOrder> declined = withRequest(seeded, CancellationRequestStatus.DECLINED);
		assertThat(approved).hasSize(scale.of(OrderSeedConstants.APPROVED_REQUESTS_BASE)).allSatisfy(order -> {
			SeededRequest request = order.request();
			SeededStep cancellation = order.history().getLast();
			assertThat(order.placedAt()).isBefore(recentWindowStart());
			assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
			assertThat(order.cancelledBy()).isEqualTo(ActorSide.CUSTOMER);
			assertThat(order.cancelReason()).isEqualTo(request.reason());
			assertThat(order.cancelNote()).isEqualTo(request.note());
			assertThat(request.reason().allows(ActorSide.CUSTOMER)).isTrue();
			assertThat(cancellation.fromStatus()).isEqualTo(OrderStatus.PREPARING);
			assertThat(cancellation.actorSide()).isEqualTo(ActorSide.RESTAURANT);
			assertThat(cancellation.actorRole()).isEqualTo(Role.STAFF);
			assertThat(cancellation.actorId()).isEqualTo(request.decidedById());
			assertThat(cancellation.reason()).isEqualTo(request.reason());
			assertThat(cancellation.occurredAt()).isEqualTo(request.decidedAt());
			assertDecidedInTime(order, request);
		});
		assertThat(declined).hasSize(scale.of(OrderSeedConstants.DECLINED_REQUESTS_BASE)).allSatisfy(order -> {
			SeededRequest request = order.request();
			assertThat(order.placedAt()).isBefore(recentWindowStart());
			assertThat(order.status()).isEqualTo(OrderStatus.DELIVERED);
			assertThat(order.cancelledBy()).isNull();
			assertThat(request.decidedByRole()).isEqualTo(Role.STAFF);
			assertThat(request.decisionNote()).isNotBlank();
			assertThat(request.decidedAt()).isBefore(stepTo(order, OrderStatus.OUT_FOR_DELIVERY).occurredAt());
			assertDecidedInTime(order, request);
		});
	}

	@Test
	void orderSeederIsStillIdempotent() {
		SeedRunner runner = new SeedRunner(List.of(seeder), new SeedProperties(SeedScale.SMALL, RANDOM_SEED),
				transactions);
		boolean seededBefore = seeder.alreadySeeded();
		runner.run(new DefaultApplicationArguments());
		long ordersAfterFirstRun = orderRepository.count();
		long requestsAfterFirstRun = cancellationRequestRepository.count();

		runner.run(new DefaultApplicationArguments());

		assertThat(seededBefore).isFalse();
		assertThat(seeder.alreadySeeded()).isTrue();
		assertThat(orderRepository.count()).isEqualTo(ordersAfterFirstRun);
		assertThat(cancellationRequestRepository.count()).isPositive().isEqualTo(requestsAfterFirstRun);
	}

	private void assertDecidedInTime(SeededOrder order, SeededRequest request) {
		assertThat(request.requestedAt()).isAfter(stepTo(order, OrderStatus.PREPARING).occurredAt());
		assertThat(request.decidedAt()).isAfter(request.requestedAt())
			.isBeforeOrEqualTo(request.requestedAt().plus(DECISION_WINDOW));
	}

	private List<SeededOrder> withRequest(List<SeededOrder> seeded, CancellationRequestStatus status) {
		return seeded.stream()
			.filter(order -> Optional.ofNullable(order.request()).map(SeededRequest::status).orElse(null) == status)
			.toList();
	}

	private SeededOrder withStatus(List<SeededOrder> seeded, OrderStatus status) {
		return seeded.stream().filter(order -> order.status() == status).findFirst().orElseThrow();
	}

	private SeededStep stepTo(SeededOrder order, OrderStatus status) {
		return order.history().stream().filter(step -> step.toStatus() == status).findFirst().orElseThrow();
	}

	private MvcTestResult myOrder(User customer, UUID orderId) {
		return mvc.get()
			.uri(RouteConstants.MY_ORDERS_PATH + RouteConstants.ID_PATH, orderId)
			.header(HttpHeaders.AUTHORIZATION, tokens.bearer(customer))
			.exchange();
	}

	private void seed(SeedScale scale) {
		transactions.executeWithoutResult(status -> seeder.seed(scale));
	}

	private List<SeededOrder> seededOrders() {
		return transactions.execute(status -> {
			Map<UUID, CancellationRequest> requests = cancellationRequestRepository.findAll()
				.stream()
				.collect(Collectors.toMap(request -> request.getOrder().getId(), Function.identity()));
			return orderRepository.findAll()
				.stream()
				.map(order -> SeededOrder.of(order, requests.get(order.getId())))
				.sorted(Comparator.comparing(SeededOrder::placedAt).thenComparing(SeededOrder::number))
				.toList();
		});
	}

	private Instant recentWindowStart() {
		return NOW.minus(OrderSeedConstants.RECENT_WINDOW);
	}

	private boolean placedAtPeak(SeededOrder order) {
		LocalTime time = LocalTime.ofInstant(order.placedAt(), restaurant.timeZone());
		boolean atLunch = !time.isBefore(LUNCH_START) && time.isBefore(LUNCH_END);
		boolean atDinner = !time.isBefore(DINNER_START) && time.isBefore(DINNER_END);
		return atLunch || atDinner;
	}

	private int percentOf(long part, long whole) {
		return (int) (part * SeedConstants.PERCENT / whole);
	}

	record SeededOrder(UUID id, long number, Instant placedAt, OrderStatus status, UUID customerId,
			String customerEmail, Role customerRole, boolean customerActive, BigDecimal total, BigDecimal itemsTotal,
			String zipCode, BrazilianState state, List<SeededItem> items, List<SeededStep> history,
			@Nullable ActorSide cancelledBy, @Nullable CancellationReason cancelReason, @Nullable String cancelNote,
			@Nullable SeededRequest request) {

		static SeededOrder of(Order order, @Nullable CancellationRequest request) {
			User customer = order.getCustomer();
			List<SeededItem> items = order.getItems()
				.stream()
				.sorted(Comparator.comparing(OrderItem::getId))
				.map(SeededItem::of)
				.toList();
			BigDecimal itemsTotal = items.stream().map(SeededItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
			List<SeededStep> history = order.getHistory()
				.stream()
				.sorted(Comparator.comparing(OrderStatusHistory::getId))
				.map(SeededStep::of)
				.toList();
			return new SeededOrder(order.getId(), order.getNumber(), order.getPlacedAt(), order.getStatus(),
					customer.getId(), customer.getEmail(), customer.getRole(), customer.isActive(), order.getTotal(),
					itemsTotal, order.getDeliveryAddress().getZipCode(), order.getDeliveryAddress().getState(), items,
					history, order.getCancelledBy(), order.getCancelReason(), order.getCancelNote(),
					SeededRequest.of(request));
		}

	}

	record SeededRequest(CancellationRequestStatus status, CancellationReason reason, @Nullable String note,
			Instant requestedAt, @Nullable Instant decidedAt, @Nullable UUID decidedById, @Nullable Role decidedByRole,
			@Nullable String decisionNote) {

		static @Nullable SeededRequest of(@Nullable CancellationRequest request) {
			if (request == null) {
				return null;
			}
			Optional<User> decider = Optional.ofNullable(request.getDecidedBy());
			return new SeededRequest(request.getStatus(), request.getReason(), request.getNote(),
					request.getRequestedAt(), request.getDecidedAt(), decider.map(User::getId).orElse(null),
					decider.map(User::getRole).orElse(null), request.getDecisionNote());
		}

	}

	record SeededItem(UUID productId, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {

		static SeededItem of(OrderItem item) {
			return new SeededItem(item.getProductId(), item.getUnitPrice(), item.getQuantity(), item.getSubtotal());
		}

	}

	record SeededStep(@Nullable OrderStatus fromStatus, OrderStatus toStatus, Instant occurredAt, ActorSide actorSide,
			UUID actorId, Role actorRole, @Nullable CancellationReason reason, @Nullable String note) {

		static SeededStep of(OrderStatusHistory step) {
			return new SeededStep(step.getFromStatus(), step.getToStatus(), step.getOccurredAt(), step.getActorSide(),
					step.getActor().getId(), step.getActor().getRole(), step.getReason(), step.getNote());
		}

	}

}
