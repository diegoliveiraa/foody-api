package com.foody.api.order.repository;

public final class OrderMetricsQueries {

	public static final String TOTALS = """
			select count(*) as "placed",
			       count(*) filter (where o.status = :delivered) as "delivered",
			       coalesce(sum(o.total) filter (where o.status = :delivered), 0) as "revenue",
			       count(*) filter (where o.status = :cancelled) as "cancelled"
			from orders o
			where o.placed_at >= :windowStart and o.placed_at < :windowEnd
			""";

	public static final String COUNT_PLACED = """
			select count(*)
			from orders o
			where o.placed_at >= :windowStart and o.placed_at < :windowEnd
			""";

	public static final String PLACED_PER_BUCKET = """
			select date_trunc(:unit, o.placed_at, :zone) as "bucketStart", count(*) as "orderCount"
			from orders o
			where o.placed_at >= :windowStart and o.placed_at < :windowEnd
			group by 1
			order by 1
			""";

	public static final String TOP_PRODUCTS = """
			select i.product_id as "productId",
			       (array_agg(i.product_name order by o.placed_at desc, o.number desc))[1] as "name",
			       sum(i.quantity) as "quantity",
			       sum(i.subtotal) as "revenue"
			from order_items i
			join orders o on o.id = i.order_id
			where o.status = :status and o.placed_at >= :windowStart and o.placed_at < :windowEnd
			group by i.product_id
			order by "revenue" desc, "quantity" desc, "name"
			limit :maxRows
			""";

	public static final String CANCELLATIONS = """
			select o.cancel_reason as "reason", o.cancelled_by as "cancelledBy", count(*) as "orderCount"
			from orders o
			where o.status = :status and o.placed_at >= :windowStart and o.placed_at < :windowEnd
			group by o.cancel_reason, o.cancelled_by
			""";

	public static final String STATUS_COUNTS = """
			select o.status as "status", count(*) as "orderCount"
			from orders o
			where o.status in (:statuses)
			group by o.status
			""";

	public static final String STAGE_DURATIONS = """
			select h.from_status as "fromStatus", h.to_status as "toStatus",
			       avg(extract(epoch from (h.occurred_at - entered.occurred_at))) as "averageSeconds"
			from order_status_history h
			join order_status_history entered on entered.order_id = h.order_id and entered.to_status = h.from_status
			where h.to_status in (:targets) and h.occurred_at >= :windowStart and h.occurred_at < :windowEnd
			group by h.from_status, h.to_status
			""";

	public static final String COUNT_CANCELLATION_REQUESTS = """
			select count(*)
			from cancellation_requests r
			where r.status = :status
			""";

	public static final String OLDEST_CANCELLATION_REQUESTS = """
			select r.order_id as "orderId", o.number as "number", r.requested_at as "requestedAt"
			from cancellation_requests r
			join orders o on o.id = r.order_id
			where r.status = :status
			order by r.requested_at, o.number
			limit :maxRows
			""";

	private OrderMetricsQueries() {
	}

}
