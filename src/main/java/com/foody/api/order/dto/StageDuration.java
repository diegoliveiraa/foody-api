package com.foody.api.order.dto;

import com.foody.api.order.entity.OrderStatus;
import java.math.BigDecimal;

public record StageDuration(OrderStatus from, OrderStatus to, BigDecimal averageSeconds) {

	public static StageDuration of(StageDurationRow row) {
		return new StageDuration(row.getFromStatus(), row.getToStatus(), row.getAverageSeconds());
	}

}
