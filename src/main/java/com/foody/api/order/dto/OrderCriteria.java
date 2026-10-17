package com.foody.api.order.dto;

import com.foody.api.order.entity.OrderStatus;
import com.foody.api.shared.restaurant.RestaurantProperties;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record OrderCriteria(@Nullable Set<OrderStatus> statuses, @Nullable Instant placedFrom,
		@Nullable Instant placedBefore, @Nullable String search) {

	public static OrderCriteria of(StaffOrderFilter filter, RestaurantProperties restaurant) {
		Set<OrderStatus> statuses = Optional.ofNullable(filter.statuses())
			.filter(values -> !values.isEmpty())
			.map(EnumSet::copyOf)
			.orElse(null);
		Instant placedFrom = Optional.ofNullable(filter.from()).map(restaurant::startOf).orElse(null);
		Instant placedBefore = Optional.ofNullable(filter.to()).map(restaurant::startOfDayAfter).orElse(null);
		return new OrderCriteria(statuses, placedFrom, placedBefore, filter.search());
	}

}
