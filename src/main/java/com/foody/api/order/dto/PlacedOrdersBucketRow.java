package com.foody.api.order.dto;

import java.time.Instant;

public interface PlacedOrdersBucketRow {

	Instant getBucketStart();

	long getOrderCount();

}
