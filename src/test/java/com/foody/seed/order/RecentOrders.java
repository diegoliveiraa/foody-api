package com.foody.seed.order;

import com.foody.api.order.entity.OrderStatus;
import com.foody.seed.SeedScale;

public enum RecentOrders {

	JUST_RECEIVED(OrderStatus.RECEIVED, new MinuteRange(1, 10), OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE,
			false),
	IN_THE_KITCHEN(OrderStatus.PREPARING, new MinuteRange(10, 35), OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE,
			true),
	ON_THE_WAY(OrderStatus.OUT_FOR_DELIVERY, new MinuteRange(40, 65), OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE,
			false),
	JUST_DELIVERED(OrderStatus.DELIVERED, new MinuteRange(75, 115), OrderSeedConstants.RECENTLY_DELIVERED_BASE, false);

	private final OrderStatus status;

	private final MinuteRange age;

	private final int baseCount;

	private final boolean holdsFixedCustomerOrder;

	RecentOrders(OrderStatus status, MinuteRange age, int baseCount, boolean holdsFixedCustomerOrder) {
		this.status = status;
		this.age = age;
		this.baseCount = baseCount;
		this.holdsFixedCustomerOrder = holdsFixedCustomerOrder;
	}

	public OrderStatus status() {
		return status;
	}

	public MinuteRange age() {
		return age;
	}

	public int countAt(SeedScale scale) {
		return Math.min(scale.of(baseCount), OrderSeedConstants.MAX_ORDERS_PER_RECENT_STATUS);
	}

	public boolean holdsFixedCustomerOrder() {
		return holdsFixedCustomerOrder;
	}

}
