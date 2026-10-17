package com.foody.seed.order;

import com.foody.api.catalog.dto.CartItem;
import com.foody.api.catalog.dto.CartQuote;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.service.CartPricingService;
import com.foody.api.order.entity.DeliveryAddress;
import com.foody.api.order.entity.Order;
import com.foody.api.order.repository.OrderRepository;
import com.foody.api.shared.persistence.PersistenceConstants;
import com.foody.api.user.entity.User;
import com.foody.seed.EntityFactory;
import com.foody.seed.SeedConstants;
import com.foody.seed.catalog.ProductFactory;
import com.foody.seed.user.UserFactory;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import net.datafaker.Faker;
import org.jspecify.annotations.Nullable;

public class OrderFactory extends EntityFactory<Order> {

	private final OrderRepository orders;

	private final UserFactory users;

	private final ProductFactory products;

	private final CartPricingService cartPricing;

	private final Faker faker;

	private final Clock clock;

	public OrderFactory(OrderRepository orders, UserFactory users, ProductFactory products,
			CartPricingService cartPricing, Faker faker, Clock clock) {
		super(orders);
		this.orders = orders;
		this.users = users;
		this.products = products;
		this.cartPricing = cartPricing;
		this.faker = faker;
		this.clock = clock;
	}

	@Override
	public Order build() {
		return build(users.create(), now());
	}

	public Order build(User customer, Instant placedAt) {
		return build(customer, List.of(new CartItem(products.create().getId(), quantity())), deliveryAddress(),
				placedAt);
	}

	public Order build(User customer, List<CartItem> items, DeliveryAddress address, Instant placedAt) {
		CartQuote quote = cartPricing.quote(items);
		Order order = new Order(orders.nextNumber(), customer, address, quote.total(), placedAt);
		quote.items()
			.forEach(item -> order.addItem(item.productId(), item.name(), item.unitPrice(), item.quantity(),
					item.subtotal()));
		return order;
	}

	public Order create(User customer) {
		return save(build(customer, now()));
	}

	public Order create(User customer, Instant placedAt) {
		return save(build(customer, placedAt));
	}

	public Order create(User customer, Instant placedAt, Consumer<Order> customizer) {
		Order order = build(customer, placedAt);
		customizer.accept(order);
		return save(order);
	}

	public Order create(User customer, List<CartItem> items, Instant placedAt, Consumer<Order> customizer) {
		Order order = build(customer, items, deliveryAddress(), placedAt);
		customizer.accept(order);
		return save(order);
	}

	public Order create(User customer, Product product) {
		return save(build(customer, List.of(new CartItem(product.getId(), quantity())), deliveryAddress(), now()));
	}

	public DeliveryAddress deliveryAddress() {
		int zipCode = faker.number().numberBetween(OrderSeedConstants.ZIP_CODE_MIN, OrderSeedConstants.ZIP_CODE_MAX);
		String formattedZipCode = OrderSeedConstants.ZIP_CODE_FORMAT.formatted(
				zipCode / OrderSeedConstants.ZIP_CODE_SUFFIX_DIVISOR,
				zipCode % OrderSeedConstants.ZIP_CODE_SUFFIX_DIVISOR);
		String number = String.valueOf(faker.number()
			.numberBetween(OrderSeedConstants.ADDRESS_NUMBER_MIN, OrderSeedConstants.ADDRESS_NUMBER_MAX));
		return new DeliveryAddress(formattedZipCode, faker.address().streetName(), number, complement(),
				faker.options().nextElement(OrderSeedConstants.NEIGHBORHOODS), OrderSeedConstants.CITY,
				OrderSeedConstants.STATE);
	}

	public int quantity() {
		return faker.options().nextElement(OrderSeedConstants.QUANTITIES);
	}

	private @Nullable String complement() {
		if (faker.number().numberBetween(0, SeedConstants.PERCENT) >= OrderSeedConstants.COMPLEMENT_PERCENT) {
			return null;
		}
		return faker.address().secondaryAddress();
	}

	private Instant now() {
		return clock.instant().truncatedTo(PersistenceConstants.TIMESTAMP_PRECISION);
	}

}
