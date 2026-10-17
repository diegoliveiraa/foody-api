package com.foody.api.dashboard.dto;

import java.time.Instant;

public record DashboardBucket(Instant bucketStart, long count) {
}
