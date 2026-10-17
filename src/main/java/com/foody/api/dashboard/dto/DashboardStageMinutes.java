package com.foody.api.dashboard.dto;

import com.foody.api.dashboard.util.DashboardMathUtils;
import com.foody.api.order.dto.StageDuration;
import com.foody.api.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public record DashboardStageMinutes(@Nullable Long receivedToPreparing, @Nullable Long preparingToOutForDelivery,
		@Nullable Long outForDeliveryToDelivered) {

	public static DashboardStageMinutes of(List<StageDuration> stages) {
		Map<OrderStatus, BigDecimal> secondsBySource = stages.stream()
			.collect(Collectors.toMap(StageDuration::from, StageDuration::averageSeconds));
		return new DashboardStageMinutes(DashboardMathUtils.roundedMinutes(secondsBySource.get(OrderStatus.RECEIVED)),
				DashboardMathUtils.roundedMinutes(secondsBySource.get(OrderStatus.PREPARING)),
				DashboardMathUtils.roundedMinutes(secondsBySource.get(OrderStatus.OUT_FOR_DELIVERY)));
	}

}
