package com.foody.api.order.repository;

import com.foody.api.order.dto.CancellationCountRow;
import com.foody.api.order.dto.OrderTotalsRow;
import com.foody.api.order.dto.PendingCancellationRow;
import com.foody.api.order.dto.PlacedOrdersBucketRow;
import com.foody.api.order.dto.ProductSalesRow;
import com.foody.api.order.dto.StageDurationRow;
import com.foody.api.order.dto.StatusCountRow;
import com.foody.api.order.entity.Order;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface OrderMetricsRepository extends Repository<Order, UUID> {

	@Query(value = OrderMetricsQueries.TOTALS, nativeQuery = true)
	OrderTotalsRow totals(Instant windowStart, Instant windowEnd, String delivered, String cancelled);

	@Query(value = OrderMetricsQueries.COUNT_PLACED, nativeQuery = true)
	long countPlaced(Instant windowStart, Instant windowEnd);

	@Query(value = OrderMetricsQueries.PLACED_PER_BUCKET, nativeQuery = true)
	List<PlacedOrdersBucketRow> countPlacedPerBucket(Instant windowStart, Instant windowEnd, String unit, String zone);

	@Query(value = OrderMetricsQueries.TOP_PRODUCTS, nativeQuery = true)
	List<ProductSalesRow> topProducts(Instant windowStart, Instant windowEnd, String status, int maxRows);

	@Query(value = OrderMetricsQueries.CANCELLATIONS, nativeQuery = true)
	List<CancellationCountRow> countCancellations(Instant windowStart, Instant windowEnd, String status);

	@Query(value = OrderMetricsQueries.STATUS_COUNTS, nativeQuery = true)
	List<StatusCountRow> countPerStatus(Collection<String> statuses);

	@Query(value = OrderMetricsQueries.STAGE_DURATIONS, nativeQuery = true)
	List<StageDurationRow> averageStageDurations(Instant windowStart, Instant windowEnd, Collection<String> targets);

	@Query(value = OrderMetricsQueries.COUNT_CANCELLATION_REQUESTS, nativeQuery = true)
	long countCancellationRequests(String status);

	@Query(value = OrderMetricsQueries.OLDEST_CANCELLATION_REQUESTS, nativeQuery = true)
	List<PendingCancellationRow> oldestCancellationRequests(String status, int maxRows);

}
