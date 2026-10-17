package com.foody.seed.order;

import com.foody.api.catalog.dto.CartItem;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.repository.ProductRepository;
import com.foody.api.order.entity.CancellationRequest;
import com.foody.api.order.entity.Order;
import com.foody.api.order.repository.CancellationRequestRepository;
import com.foody.api.order.repository.OrderRepository;
import com.foody.api.shared.persistence.ActivationStatus;
import com.foody.api.shared.persistence.PersistenceConstants;
import com.foody.api.shared.restaurant.RestaurantProperties;
import com.foody.api.shared.security.Role;
import com.foody.api.user.UserConstants;
import com.foody.api.user.dto.AdminUserFilter;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.api.user.repository.UserSpecifications;
import com.foody.seed.SeedScale;
import com.foody.seed.Seeder;
import com.foody.seed.user.IdentitySeedProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.datafaker.Faker;
import org.springframework.data.domain.Sort;

@org.springframework.core.annotation.Order(3)
public class OrderSeeder implements Seeder {

	private final OrderRepository orderRepository;

	private final CancellationRequestRepository cancellationRequestRepository;

	private final UserRepository userRepository;

	private final ProductRepository productRepository;

	private final OrderFactory orders;

	private final Faker faker;

	private final Clock clock;

	private final IdentitySeedProperties identity;

	private final OrderPlanner planner;

	private final OrderStory story;

	public OrderSeeder(OrderRepository orderRepository, CancellationRequestRepository cancellationRequestRepository,
			UserRepository userRepository, ProductRepository productRepository, OrderFactory orders, Faker faker,
			Clock clock, RestaurantProperties restaurant, IdentitySeedProperties identity) {
		this.orderRepository = orderRepository;
		this.cancellationRequestRepository = cancellationRequestRepository;
		this.userRepository = userRepository;
		this.productRepository = productRepository;
		this.orders = orders;
		this.faker = faker;
		this.clock = clock;
		this.identity = identity;
		this.planner = new OrderPlanner(faker, restaurant.timeZone());
		this.story = new OrderStory(faker);
	}

	@Override
	public boolean alreadySeeded() {
		return orderRepository.count() > 0;
	}

	@Override
	public void seed(SeedScale scale) {
		Instant now = clock.instant().truncatedTo(PersistenceConstants.TIMESTAMP_PRECISION);
		SeedCast cast = cast();
		planner.plan(scale, now).forEach(plan -> seedOrder(plan, cast));
	}

	private SeedCast cast() {
		User fixedCustomer = userRepository.findByEmail(identity.customer().email())
			.filter(User::isActive)
			.orElse(null);
		List<User> customers = activeUsers(Role.CUSTOMER).stream()
			.filter(customer -> !customer.equals(fixedCustomer))
			.toList();
		return new SeedCast(fixedCustomer, customers, activeUsers(Role.STAFF), productRepository.findAllOnMenu());
	}

	private List<User> activeUsers(Role role) {
		AdminUserFilter filter = new AdminUserFilter(null, role, ActivationStatus.ACTIVE);
		return userRepository.findAll(UserSpecifications.matching(filter), Sort.by(UserConstants.EMAIL_ATTRIBUTE));
	}

	private void seedOrder(OrderPlan plan, SeedCast cast) {
		User customer = customerFor(plan, cast);
		Order order = orders.build(customer, itemsFrom(cast.menu()), orders.deliveryAddress(), plan.placedAt());
		Optional<CancellationRequest> request = story.playOut(order, plan, customer, cast.staff());
		orderRepository.save(order);
		request.ifPresent(cancellationRequestRepository::save);
	}

	private User customerFor(OrderPlan plan, SeedCast cast) {
		User fixedCustomer = cast.fixedCustomer();
		if (plan.forFixedCustomer() && fixedCustomer != null) {
			return fixedCustomer;
		}
		return faker.options().nextElement(cast.customers());
	}

	private List<CartItem> itemsFrom(List<Product> menu) {
		int count = Math.min(faker.options().nextElement(OrderSeedConstants.ITEM_COUNTS), menu.size());
		return Stream.generate(() -> faker.number().numberBetween(0, menu.size()))
			.distinct()
			.limit(count)
			.map(index -> new CartItem(menu.get(index).getId(), orders.quantity()))
			.toList();
	}

}
