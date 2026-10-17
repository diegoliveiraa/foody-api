package com.foody.api.order.service;

import com.foody.api.catalog.dto.CartQuote;
import com.foody.api.catalog.dto.QuotedItem;
import com.foody.api.catalog.service.CartPricingService;
import com.foody.api.order.OrderConstants;
import com.foody.api.order.dto.CancelOrderRequest;
import com.foody.api.order.dto.CustomerOrderDetailResponse;
import com.foody.api.order.dto.CustomerOrderSummaryResponse;
import com.foody.api.order.dto.PlaceOrderRequest;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationRequest;
import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderItem;
import com.foody.api.order.exception.OrderNotFoundException;
import com.foody.api.order.exception.ProductUnavailableException;
import com.foody.api.order.repository.CancellationRequestRepository;
import com.foody.api.order.repository.OrderRepository;
import com.foody.api.shared.pagination.PageRequests;
import com.foody.api.shared.persistence.PersistenceConstants;
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
public class CustomerOrderService {

	private final OrderRepository orders;

	private final OrderContentService contents;

	private final CancellationRequestRepository cancellationRequests;

	private final OrderLifecyclePolicy lifecycle;

	private final CartPricingService cartPricing;

	private final UserService users;

	private final Clock clock;

	public CustomerOrderService(OrderRepository orders, OrderContentService contents,
			CancellationRequestRepository cancellationRequests, OrderLifecyclePolicy lifecycle,
			CartPricingService cartPricing, UserService users, Clock clock) {
		this.orders = orders;
		this.contents = contents;
		this.cancellationRequests = cancellationRequests;
		this.lifecycle = lifecycle;
		this.cartPricing = cartPricing;
		this.users = users;
		this.clock = clock;
	}

	@Transactional
	public CustomerOrderDetailResponse place(UUID customerId, PlaceOrderRequest request) {
		CartQuote quote = cartPricing.quote(request.items());
		requireAvailable(quote);
		User customer = users.getActive(customerId);
		Order order = new Order(orders.nextNumber(), customer, request.deliveryAddress().toDeliveryAddress(),
				quote.total(), now());
		quote.items()
			.forEach(item -> order.addItem(item.productId(), item.name(), item.unitPrice(), item.quantity(),
					item.subtotal()));
		Order saved = orders.save(order);
		return CustomerOrderDetailResponse.of(saved, saved.getItems(), saved.getHistory(), null,
				lifecycle.customerActions(saved.getStatus(), null));
	}

	@Transactional(readOnly = true)
	public Page<CustomerOrderSummaryResponse> list(UUID customerId, Pageable pageable) {
		Pageable sorted = PageRequests.sortedOrDefault(pageable, OrderConstants.DEFAULT_SORT);
		Page<Order> page = orders.findAllPlacedBy(customerId, sorted);
		Map<UUID, List<OrderItem>> items = contents.itemsByOrder(page.getContent());
		Map<UUID, CancellationRequestStatus> requests = contents.cancellationRequestStatusesOf(page.getContent());
		return page.map(order -> CustomerOrderSummaryResponse.of(order, items.getOrDefault(order.getId(), List.of()),
				requests.get(order.getId())));
	}

	@Transactional(readOnly = true)
	public CustomerOrderDetailResponse detail(UUID customerId, UUID orderId) {
		Order order = orders.findByIdAndCustomerId(orderId, customerId)
			.orElseThrow(() -> new OrderNotFoundException(orderId));
		return detailOf(order, contents.cancellationRequestOf(order));
	}

	@Transactional
	public CustomerOrderDetailResponse cancel(UUID customerId, UUID orderId, CancelOrderRequest cancellation) {
		Order order = lockedOrderOf(customerId, orderId);
		Optional<CancellationRequest> request = contents.cancellationRequestOf(order);
		lifecycle.requireCancellation(ActorSide.CUSTOMER, order.getStatus(), statusOf(request), cancellation.reason());
		order.cancel(order.getCustomer(), ActorSide.CUSTOMER, cancellation.reason(), cancellation.note(), now());
		return detailOf(order, request);
	}

	@Transactional
	public CustomerOrderDetailResponse requestCancellation(UUID customerId, UUID orderId,
			CancelOrderRequest cancellation) {
		Order order = lockedOrderOf(customerId, orderId);
		lifecycle.requireCancellationRequest(order.getStatus(), statusOf(contents.cancellationRequestOf(order)),
				cancellation.reason());
		CancellationRequest request = cancellationRequests
			.save(new CancellationRequest(order, cancellation.reason(), cancellation.note(), now()));
		return detailOf(order, Optional.of(request));
	}

	private Order lockedOrderOf(UUID customerId, UUID orderId) {
		return orders.lockById(orderId)
			.filter(order -> order.isPlacedBy(customerId))
			.orElseThrow(() -> new OrderNotFoundException(orderId));
	}

	private CustomerOrderDetailResponse detailOf(Order order, Optional<CancellationRequest> request) {
		return CustomerOrderDetailResponse.of(order, contents.itemsOf(order), contents.historyOf(order),
				request.orElse(null), lifecycle.customerActions(order.getStatus(), statusOf(request)));
	}

	private @Nullable CancellationRequestStatus statusOf(Optional<CancellationRequest> request) {
		return request.map(CancellationRequest::getStatus).orElse(null);
	}

	private Instant now() {
		return clock.instant().truncatedTo(PersistenceConstants.TIMESTAMP_PRECISION);
	}

	private void requireAvailable(CartQuote quote) {
		List<String> unavailable = quote.items().stream().filter(item -> !item.available()).map(this::labelOf).toList();
		if (unavailable.isEmpty()) {
			return;
		}
		throw new ProductUnavailableException(unavailable);
	}

	private String labelOf(QuotedItem item) {
		return Optional.ofNullable(item.name()).orElseGet(() -> item.productId().toString());
	}

}
