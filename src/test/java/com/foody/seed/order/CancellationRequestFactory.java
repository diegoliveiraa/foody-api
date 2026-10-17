package com.foody.seed.order;

import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationRequest;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.repository.CancellationRequestRepository;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.seed.EntityFactory;
import com.foody.seed.user.UserFactory;
import java.time.Instant;
import java.util.function.Consumer;
import net.datafaker.Faker;

public class CancellationRequestFactory extends EntityFactory<CancellationRequest> {

	private final OrderFactory orders;

	private final UserFactory users;

	private final Faker faker;

	public CancellationRequestFactory(CancellationRequestRepository repository, OrderFactory orders, UserFactory users,
			Faker faker) {
		super(repository);
		this.orders = orders;
		this.users = users;
		this.faker = faker;
	}

	@Override
	public CancellationRequest build() {
		User staff = users.create(user -> user.changeRole(Role.STAFF));
		Order order = orders.create(placed -> placed.advance(OrderStatus.PREPARING, staff, ActorSide.RESTAURANT,
				placed.getPlacedAt().plus(OrderSeedConstants.STEP_DELAYS.get(OrderStatus.PREPARING).draw(faker))));
		return build(order);
	}

	public CancellationRequest build(Order order) {
		Instant lastStep = order.getHistory().getLast().getOccurredAt();
		return new CancellationRequest(order, faker.options().nextElement(OrderSeedConstants.CUSTOMER_REASONS), null,
				lastStep.plus(OrderSeedConstants.REQUEST_DELAY.draw(faker)));
	}

	public CancellationRequest create(Order order) {
		return save(build(order));
	}

	public CancellationRequest create(Order order, Consumer<CancellationRequest> customizer) {
		CancellationRequest request = build(order);
		customizer.accept(request);
		return save(request);
	}

}
