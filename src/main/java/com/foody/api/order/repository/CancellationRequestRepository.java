package com.foody.api.order.repository;

import com.foody.api.order.dto.CancellationRequestState;
import com.foody.api.order.entity.CancellationRequest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CancellationRequestRepository extends JpaRepository<CancellationRequest, UUID> {

	@Query("select r from CancellationRequest r left join fetch r.decidedBy where r.order.id = :orderId")
	Optional<CancellationRequest> findWithDeciderOfOrder(UUID orderId);

	@Query("""
			select new com.foody.api.order.dto.CancellationRequestState(r.order.id, r.status)
			from CancellationRequest r
			where r.order.id in :orderIds
			""")
	List<CancellationRequestState> findStatesOfOrders(Collection<UUID> orderIds);

}
