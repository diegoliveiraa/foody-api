package com.foody.api.dashboard.dto;

import com.foody.api.dashboard.DashboardConstants;
import com.foody.api.order.entity.OrderStatus;
import java.util.Map;

public record DashboardActiveOrders(long received, long preparing, long outForDelivery) {

	public static DashboardActiveOrders of(Map<OrderStatus, Long> counts) {
		return new DashboardActiveOrders(counts.getOrDefault(OrderStatus.RECEIVED, DashboardConstants.NO_ORDERS),
				counts.getOrDefault(OrderStatus.PREPARING, DashboardConstants.NO_ORDERS),
				counts.getOrDefault(OrderStatus.OUT_FOR_DELIVERY, DashboardConstants.NO_ORDERS));
	}

}
