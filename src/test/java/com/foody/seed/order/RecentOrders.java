package com.foody.seed.order;

import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.order.entity.OrderStatus;
import com.foody.seed.SeedScale;
import java.util.Arrays;
import org.jspecify.annotations.Nullable;

public enum RecentOrders {

	JUST_RECEIVED(OrderStatus.RECEIVED, new MinuteRange(1, 10), OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE, true,
			null),
	IN_THE_KITCHEN(OrderStatus.PREPARING, new MinuteRange(10, 35), OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE,
			true, null),
	AWAITING_DECISION(OrderStatus.PREPARING, new MinuteRange(15, 35), OrderSeedConstants.PENDING_REQUESTS_BASE, false,
			CancellationRequestStatus.PENDING),
	ON_THE_WAY(OrderStatus.OUT_FOR_DELIVERY, new MinuteRange(40, 65), OrderSeedConstants.ACTIVE_ORDERS_PER_STATUS_BASE,
			false, null),
	JUST_DELIVERED(OrderStatus.DELIVERED, new MinuteRange(75, 115), OrderSeedConstants.RECENTLY_DELIVERED_BASE, false,
			null);

	private final OrderStatus status;

	private final MinuteRange age;

	private final int baseCount;

	private final boolean holdsFixedCustomerOrder;

	private final @Nullable CancellationRequestStatus cancellationRequest;

	RecentOrders(OrderStatus status, MinuteRange age, int baseCount, boolean holdsFixedCustomerOrder,
			@Nullable CancellationRequestStatus cancellationRequest) {
		this.status = status;
		this.age = age;
		this.baseCount = baseCount;
		this.holdsFixedCustomerOrder = holdsFixedCustomerOrder;
		this.cancellationRequest = cancellationRequest;
	}

	public OrderStatus status() {
		return status;
	}

	public MinuteRange age() {
		return age;
	}

	public int countAt(SeedScale scale) {
		return Math.min(scale.of(baseCount), OrderSeedConstants.MAX_ACTIVE_ORDERS / activeSlotCount());
	}

	public boolean holdsFixedCustomerOrder() {
		return holdsFixedCustomerOrder;
	}

	public @Nullable CancellationRequestStatus cancellationRequest() {
		return cancellationRequest;
	}

	private static int activeSlotCount() {
		return (int) Arrays.stream(values()).filter(slot -> slot.status.isActive()).count();
	}

}
