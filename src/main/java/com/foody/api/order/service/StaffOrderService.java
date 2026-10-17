package com.foody.api.order.service;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.dto.OrderCriteria;
import com.foody.api.order.dto.StaffOrderDetailResponse;
import com.foody.api.order.dto.StaffOrderFilter;
import com.foody.api.order.dto.StaffOrderSummaryResponse;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderItem;
import com.foody.api.order.exception.OrderNotFoundException;
import com.foody.api.order.repository.OrderRepository;
import com.foody.api.shared.pagination.PageRequests;
import com.foody.api.shared.restaurant.RestaurantProperties;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffOrderService {

	private final OrderRepository orders;

	private final OrderContentService contents;

	private final RestaurantProperties restaurant;

	public StaffOrderService(OrderRepository orders, OrderContentService contents, RestaurantProperties restaurant) {
		this.orders = orders;
		this.contents = contents;
		this.restaurant = restaurant;
	}

	@Transactional(readOnly = true)
	public Page<StaffOrderSummaryResponse> list(StaffOrderFilter filter, Pageable pageable) {
		Pageable sorted = PageRequests.sortedOrDefault(pageable, OrderConstants.DEFAULT_SORT);
		Page<Order> page = orders.findAllMatching(OrderCriteria.of(filter, restaurant), sorted);
		Map<UUID, List<OrderItem>> items = contents.itemsByOrder(page.getContent());
		return page.map(order -> StaffOrderSummaryResponse.of(order, items.getOrDefault(order.getId(), List.of())));
	}

	@Transactional(readOnly = true)
	public StaffOrderDetailResponse detail(UUID id) {
		Order order = orders.findWithCustomerById(id).orElseThrow(() -> new OrderNotFoundException(id));
		return StaffOrderDetailResponse.of(order, contents.itemsOf(order), contents.historyOf(order));
	}

}
