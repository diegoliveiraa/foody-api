package com.foody.api.order.repository;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.dto.OrderCriteria;
import com.foody.api.order.entity.Order;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

	@Query(value = "select nextval('order_number_seq')", nativeQuery = true)
	long nextNumber();

	@Override
	@EntityGraph(attributePaths = OrderConstants.CUSTOMER_ATTRIBUTE)
	Page<Order> findAll(Specification<Order> specification, Pageable pageable);

	@EntityGraph(attributePaths = OrderConstants.CUSTOMER_ATTRIBUTE)
	Optional<Order> findWithCustomerById(UUID id);

	Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);

	default Page<Order> findAllPlacedBy(UUID customerId, Pageable pageable) {
		return findAll(OrderSpecifications.placedBy(customerId), pageable);
	}

	default Page<Order> findAllMatching(OrderCriteria criteria, Pageable pageable) {
		return findAll(OrderSpecifications.matching(criteria), pageable);
	}

}
