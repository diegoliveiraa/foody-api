package com.foody.api.dashboard.dto;

import com.foody.api.dashboard.DashboardConstants;
import com.foody.api.order.dto.PlacedOrdersBucket;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record DashboardOrdersOverTime(DashboardGranularity granularity, List<DashboardBucket> points) {

	public static DashboardOrdersOverTime of(DashboardWindow window, List<PlacedOrdersBucket> buckets) {
		Map<Instant, Long> counts = buckets.stream()
			.collect(Collectors.toMap(PlacedOrdersBucket::bucketStart, PlacedOrdersBucket::count));
		List<DashboardBucket> points = window.bucketStarts()
			.stream()
			.map(bucketStart -> new DashboardBucket(bucketStart,
					counts.getOrDefault(bucketStart, DashboardConstants.NO_ORDERS)))
			.toList();
		return new DashboardOrdersOverTime(window.period().granularity(), points);
	}

}
