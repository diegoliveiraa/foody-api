package com.foody.api.order.service;

import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderItem;
import com.foody.api.order.entity.OrderStatusHistory;
import com.foody.api.order.repository.OrderItemRepository;
import com.foody.api.order.repository.OrderStatusHistoryRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderContentService {

	private final OrderItemRepository items;

	private final OrderStatusHistoryRepository history;

	public OrderContentService(OrderItemRepository items, OrderStatusHistoryRepository history) {
		this.items = items;
		this.history = history;
	}

	@Transactional(readOnly = true)
	public Map<UUID, List<OrderItem>> itemsByOrder(Collection<Order> orders) {
		if (orders.isEmpty()) {
			return Map.of();
		}
		List<UUID> orderIds = orders.stream().map(Order::getId).toList();
		return items.findAllOfOrders(orderIds).stream().collect(Collectors.groupingBy(item -> item.getOrder().getId()));
	}

	@Transactional(readOnly = true)
	public List<OrderItem> itemsOf(Order order) {
		return items.findAllOfOrders(List.of(order.getId()));
	}

	@Transactional(readOnly = true)
	public List<OrderStatusHistory> historyOf(Order order) {
		return history.findAllWithActorOfOrder(order.getId());
	}

}
