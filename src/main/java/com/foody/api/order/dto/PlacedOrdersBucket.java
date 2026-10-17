package com.foody.api.order.dto;

import java.time.Instant;

public record PlacedOrdersBucket(Instant bucketStart, long count) {

	public static PlacedOrdersBucket of(PlacedOrdersBucketRow row) {
		return new PlacedOrdersBucket(row.getBucketStart(), row.getOrderCount());
	}

}
