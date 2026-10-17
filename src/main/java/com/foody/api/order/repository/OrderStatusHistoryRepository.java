package com.foody.api.order.repository;

import com.foody.api.order.entity.OrderStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

	@Query("""
			select h from OrderStatusHistory h join fetch h.actor
			where h.order.id = :orderId
			order by h.occurredAt, h.id
			""")
	List<OrderStatusHistory> findAllWithActorOfOrder(UUID orderId);

}
