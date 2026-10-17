package com.foody.api.order.service;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.dto.CancellationCount;
import com.foody.api.order.dto.OrderTotals;
import com.foody.api.order.dto.PendingCancellation;
import com.foody.api.order.dto.PlacedOrdersBucket;
import com.foody.api.order.dto.ProductSales;
import com.foody.api.order.dto.StageDuration;
import com.foody.api.order.dto.StatusCountRow;
import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.repository.OrderMetricsRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderMetricsService {

	private final OrderMetricsRepository metrics;

	public OrderMetricsService(OrderMetricsRepository metrics) {
		this.metrics = metrics;
	}

	@Transactional(readOnly = true)
	public OrderTotals totalsBetween(Instant start, Instant end) {
		return OrderTotals.of(metrics.totals(start, end, OrderStatus.DELIVERED.name(), OrderStatus.CANCELLED.name()));
	}

	@Transactional(readOnly = true)
	public long countPlacedBetween(Instant start, Instant end) {
		return metrics.countPlaced(start, end);
	}

	@Transactional(readOnly = true)
	public List<PlacedOrdersBucket> placedPerBucket(Instant start, Instant end, ChronoUnit unit, ZoneId zone) {
		return metrics.countPlacedPerBucket(start, end, OrderConstants.DATE_TRUNC_FIELDS.get(unit), zone.getId())
			.stream()
			.map(PlacedOrdersBucket::of)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<ProductSales> topDeliveredProducts(Instant start, Instant end, int limit) {
		return metrics.topProducts(start, end, OrderStatus.DELIVERED.name(), limit)
			.stream()
			.map(ProductSales::of)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<CancellationCount> cancellationsBetween(Instant start, Instant end) {
		return metrics.countCancellations(start, end, OrderStatus.CANCELLED.name())
			.stream()
			.map(CancellationCount::of)
			.toList();
	}

	@Transactional(readOnly = true)
	public Map<OrderStatus, Long> countActiveByStatus() {
		return metrics.countPerStatus(namesOf(OrderConstants.ACTIVE_STATUSES))
			.stream()
			.collect(Collectors.toMap(StatusCountRow::getStatus, StatusCountRow::getOrderCount, Long::sum,
					() -> new EnumMap<>(OrderStatus.class)));
	}

	@Transactional(readOnly = true)
	public List<StageDuration> averageStageDurations(Instant start, Instant end) {
		return metrics.averageStageDurations(start, end, namesOf(OrderConstants.NEXT_STATUS.values()))
			.stream()
			.map(StageDuration::of)
			.filter(stage -> Objects.equals(OrderConstants.NEXT_STATUS.get(stage.from()), stage.to()))
			.toList();
	}

	@Transactional(readOnly = true)
	public long countPendingCancellationRequests() {
		return metrics.countCancellationRequests(CancellationRequestStatus.PENDING.name());
	}

	@Transactional(readOnly = true)
	public List<PendingCancellation> oldestPendingCancellationRequests(int limit) {
		return metrics.oldestCancellationRequests(CancellationRequestStatus.PENDING.name(), limit)
			.stream()
			.map(PendingCancellation::of)
			.toList();
	}

	private List<String> namesOf(Collection<OrderStatus> statuses) {
		return statuses.stream().map(OrderStatus::name).toList();
	}

}
