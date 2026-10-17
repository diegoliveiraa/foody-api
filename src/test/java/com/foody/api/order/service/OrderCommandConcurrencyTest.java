package com.foody.api.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.foody.api.order.dto.CancelOrderRequest;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.repository.CancellationRequestRepository;
import com.foody.api.order.repository.OrderRepository;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.seed.order.OrderFactory;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@ApiIntegrationTest
class OrderCommandConcurrencyTest {

	private static final Instant PLACED_AT = Instant.parse("2026-01-15T22:02:00Z");

	private static final Duration MINUTE = Duration.ofMinutes(1);

	@Autowired
	private StaffOrderService staffOrders;

	@Autowired
	private CustomerOrderService customerOrders;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private CancellationRequestRepository cancellationRequestRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private UserFactory users;

	@Autowired
	private OrderFactory orders;

	@Test
	void concurrentCommandsOnTheSameOrderConflict() {
		User customer = users.create();
		User staff = users.create(user -> user.changeRole(Role.STAFF));
		Order order = orders.create(customer, PLACED_AT,
				placed -> placed.advance(OrderStatus.PREPARING, staff, ActorSide.RESTAURANT, PLACED_AT.plus(MINUTE)));
		TransactionTemplate customerCommand = new TransactionTemplate(transactionManager);
		TransactionTemplate staffCommand = requiresNew();

		Throwable thrown = catchThrowable(() -> customerCommand.executeWithoutResult(status -> {
			orderRepository.lockById(order.getId());
			staffCommand.executeWithoutResult(
					inner -> staffOrders.updateStatus(staff.getId(), order.getId(), OrderStatus.OUT_FOR_DELIVERY));
			customerOrders.requestCancellation(customer.getId(), order.getId(),
					new CancelOrderRequest(CancellationReason.TAKING_TOO_LONG, null));
		}));

		assertThat(thrown).isInstanceOf(ObjectOptimisticLockingFailureException.class);
		assertThat(orderRepository.findById(order.getId())).map(Order::getStatus)
			.contains(OrderStatus.OUT_FOR_DELIVERY);
		assertThat(cancellationRequestRepository.findWithDeciderOfOrder(order.getId())).isEmpty();
	}

	@Test
	void concurrentCancellationsOfTheSameOrderConflict() {
		User customer = users.create();
		User staff = users.create(user -> user.changeRole(Role.STAFF));
		Order order = orders.create(customer, PLACED_AT);
		TransactionTemplate staffCommand = new TransactionTemplate(transactionManager);
		TransactionTemplate customerCommand = requiresNew();

		Throwable thrown = catchThrowable(() -> staffCommand.executeWithoutResult(status -> {
			orderRepository.lockById(order.getId());
			customerCommand.executeWithoutResult(inner -> customerOrders.cancel(customer.getId(), order.getId(),
					new CancelOrderRequest(CancellationReason.CHANGED_MIND, null)));
			staffOrders.cancel(staff.getId(), order.getId(),
					new CancelOrderRequest(CancellationReason.STORE_CLOSED, null));
		}));

		assertThat(thrown).isInstanceOf(ObjectOptimisticLockingFailureException.class);
		assertThat(orderRepository.findById(order.getId())).map(Order::getCancelledBy).contains(ActorSide.CUSTOMER);
		assertThat(orderRepository.findById(order.getId())).map(Order::getCancelReason)
			.contains(CancellationReason.CHANGED_MIND);
	}

	private TransactionTemplate requiresNew() {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template;
	}

}
