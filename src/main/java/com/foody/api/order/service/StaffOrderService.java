package com.foody.api.order.service;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.dto.CancelOrderRequest;
import com.foody.api.order.dto.CancellationDecisionRequest;
import com.foody.api.order.dto.OrderCriteria;
import com.foody.api.order.dto.StaffOrderDetailResponse;
import com.foody.api.order.dto.StaffOrderFilter;
import com.foody.api.order.dto.StaffOrderSummaryResponse;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationRequest;
import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderItem;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.exception.NoPendingCancellationRequestException;
import com.foody.api.order.exception.OrderNotFoundException;
import com.foody.api.order.repository.OrderRepository;
import com.foody.api.shared.pagination.PageRequests;
import com.foody.api.shared.persistence.PersistenceConstants;
import com.foody.api.shared.restaurant.RestaurantProperties;
import com.foody.api.user.entity.User;
import com.foody.api.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffOrderService {

	private final OrderRepository orders;

	private final OrderContentService contents;

	private final OrderLifecyclePolicy lifecycle;

	private final UserService users;

	private final RestaurantProperties restaurant;

	private final Clock clock;

	public StaffOrderService(OrderRepository orders, OrderContentService contents, OrderLifecyclePolicy lifecycle,
			UserService users, RestaurantProperties restaurant, Clock clock) {
		this.orders = orders;
		this.contents = contents;
		this.lifecycle = lifecycle;
		this.users = users;
		this.restaurant = restaurant;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public Page<StaffOrderSummaryResponse> list(StaffOrderFilter filter, Pageable pageable) {
		Pageable sorted = PageRequests.sortedOrDefault(pageable, OrderConstants.DEFAULT_SORT);
		Page<Order> page = orders.findAllMatching(OrderCriteria.of(filter, restaurant), sorted);
		Map<UUID, List<OrderItem>> items = contents.itemsByOrder(page.getContent());
		Map<UUID, CancellationRequestStatus> requests = contents.cancellationRequestStatusesOf(page.getContent());
		return page
			.map(order -> summaryOf(order, items.getOrDefault(order.getId(), List.of()), requests.get(order.getId())));
	}

	@Transactional(readOnly = true)
	public StaffOrderDetailResponse detail(UUID id) {
		Order order = orders.findWithCustomerById(id).orElseThrow(() -> new OrderNotFoundException(id));
		return detailOf(order, contents.cancellationRequestOf(order));
	}

	@Transactional
	public StaffOrderDetailResponse updateStatus(UUID staffId, UUID orderId, OrderStatus target) {
		Order order = lockedOrder(orderId);
		Optional<CancellationRequest> request = contents.cancellationRequestOf(order);
		lifecycle.requireTransition(order.getStatus(), statusOf(request), target);
		order.advance(target, users.getActive(staffId), ActorSide.RESTAURANT, now());
		return detailOf(order, request);
	}

	@Transactional
	public StaffOrderDetailResponse cancel(UUID staffId, UUID orderId, CancelOrderRequest cancellation) {
		Order order = lockedOrder(orderId);
		Optional<CancellationRequest> request = contents.cancellationRequestOf(order);
		lifecycle.requireCancellation(ActorSide.RESTAURANT, order.getStatus(), statusOf(request),
				cancellation.reason());
		order.cancel(users.getActive(staffId), ActorSide.RESTAURANT, cancellation.reason(), cancellation.note(), now());
		return detailOf(order, request);
	}

	@Transactional
	public StaffOrderDetailResponse approveCancellation(UUID staffId, UUID orderId,
			CancellationDecisionRequest decision) {
		Order order = lockedOrder(orderId);
		CancellationRequest request = pendingRequestOf(order);
		User staff = users.getActive(staffId);
		Instant decidedAt = now();
		request.approve(staff, decision.note(), decidedAt);
		order.cancel(staff, ActorSide.RESTAURANT, ActorSide.CUSTOMER, request.getReason(), request.getNote(),
				decidedAt);
		return detailOf(order, Optional.of(request));
	}

	@Transactional
	public StaffOrderDetailResponse declineCancellation(UUID staffId, UUID orderId,
			CancellationDecisionRequest decision) {
		Order order = lockedOrder(orderId);
		CancellationRequest request = pendingRequestOf(order);
		request.decline(users.getActive(staffId), decision.note(), now());
		return detailOf(order, Optional.of(request));
	}

	private Order lockedOrder(UUID orderId) {
		return orders.lockById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
	}

	private CancellationRequest pendingRequestOf(Order order) {
		Optional<CancellationRequest> request = contents.cancellationRequestOf(order);
		lifecycle.requireDecision(statusOf(request));
		return request.orElseThrow(NoPendingCancellationRequestException::new);
	}

	private StaffOrderSummaryResponse summaryOf(Order order, List<OrderItem> items,
			@Nullable CancellationRequestStatus request) {
		return StaffOrderSummaryResponse.of(order, items, lifecycle.hasPendingRequest(request),
				lifecycle.staffActions(order.getStatus(), request));
	}

	private StaffOrderDetailResponse detailOf(Order order, Optional<CancellationRequest> request) {
		return StaffOrderDetailResponse.of(order, contents.itemsOf(order), contents.historyOf(order),
				request.orElse(null), lifecycle.staffActions(order.getStatus(), statusOf(request)));
	}

	private @Nullable CancellationRequestStatus statusOf(Optional<CancellationRequest> request) {
		return request.map(CancellationRequest::getStatus).orElse(null);
	}

	private Instant now() {
		return clock.instant().truncatedTo(PersistenceConstants.TIMESTAMP_PRECISION);
	}

}
