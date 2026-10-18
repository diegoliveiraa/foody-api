package com.foody.api.order.repository;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.dto.OrderCriteria;
import com.foody.api.order.entity.Order;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.util.OrderSearchUtils;
import com.foody.api.shared.persistence.PersistenceConstants;
import com.foody.api.shared.util.TextSearchUtils;
import com.foody.api.user.UserConstants;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;

public final class OrderSpecifications {

	private OrderSpecifications() {
	}

	public static Specification<Order> placedBy(UUID customerId) {
		PredicateSpecification<Order> ofCustomer = (from, builder) -> builder
			.equal(from.get(OrderConstants.CUSTOMER_ATTRIBUTE).get(PersistenceConstants.ID_ATTRIBUTE), customerId);
		return Specification.where(ofCustomer);
	}

	public static Specification<Order> matching(OrderCriteria criteria) {
		List<PredicateSpecification<Order>> predicates = Stream
			.of(withStatusIn(criteria.statuses()), placedFrom(criteria.placedFrom()),
					placedBefore(criteria.placedBefore()), searching(criteria.search()))
			.flatMap(Optional::stream)
			.toList();
		return Specification.where(PredicateSpecification.allOf(predicates));
	}

	private static Optional<PredicateSpecification<Order>> withStatusIn(@Nullable Set<OrderStatus> statuses) {
		return Optional.ofNullable(statuses).filter(values -> !values.isEmpty()).map(OrderSpecifications::statusIn);
	}

	private static PredicateSpecification<Order> statusIn(Set<OrderStatus> statuses) {
		return (from, builder) -> from.get(OrderConstants.STATUS_ATTRIBUTE).in(statuses);
	}

	private static Optional<PredicateSpecification<Order>> placedFrom(@Nullable Instant start) {
		return Optional.ofNullable(start).map(OrderSpecifications::placedOnOrAfter);
	}

	private static PredicateSpecification<Order> placedOnOrAfter(Instant start) {
		return (from, builder) -> builder.greaterThanOrEqualTo(from.get(OrderConstants.PLACED_AT_ATTRIBUTE), start);
	}

	private static Optional<PredicateSpecification<Order>> placedBefore(@Nullable Instant end) {
		return Optional.ofNullable(end).map(OrderSpecifications::placedEarlierThan);
	}

	private static PredicateSpecification<Order> placedEarlierThan(Instant end) {
		return (from, builder) -> builder.lessThan(from.get(OrderConstants.PLACED_AT_ATTRIBUTE), end);
	}

	private static Optional<PredicateSpecification<Order>> searching(@Nullable String search) {
		return OrderSearchUtils.orderNumber(search)
			.map(OrderSpecifications::numberIs)
			.or(() -> TextSearchUtils.searchTerm(search).map(OrderSpecifications::customerNameLike));
	}

	private static PredicateSpecification<Order> numberIs(long number) {
		return (from, builder) -> builder.equal(from.get(OrderConstants.NUMBER_ATTRIBUTE), number);
	}

	private static PredicateSpecification<Order> customerNameLike(String term) {
		return (from, builder) -> TextSearchUtils.matches(builder,
				from.join(OrderConstants.CUSTOMER_ATTRIBUTE).get(UserConstants.NAME_ATTRIBUTE), term);
	}

}
