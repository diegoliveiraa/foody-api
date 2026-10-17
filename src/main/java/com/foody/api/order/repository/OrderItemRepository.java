package com.foody.api.order.repository;

import com.foody.api.order.entity.OrderItem;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

	@Query("select i from OrderItem i where i.order.id in :orderIds order by i.id")
	List<OrderItem> findAllOfOrders(Collection<UUID> orderIds);

	boolean existsByProductId(UUID productId);

}
