package com.foody.api.order.service;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.dto.CustomerOrderActions;
import com.foody.api.order.dto.StaffOrderActions;
import com.foody.api.order.entity.ActorSide;
import com.foody.api.order.entity.CancellationReason;
import com.foody.api.order.entity.CancellationRequestStatus;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.exception.CancellationAlreadyRequestedException;
import com.foody.api.order.exception.CancellationNotAllowedException;
import com.foody.api.order.exception.InvalidCancellationReasonException;
import com.foody.api.order.exception.InvalidStatusTransitionException;
import com.foody.api.order.exception.NoPendingCancellationRequestException;
import com.foody.api.order.exception.PendingCancellationRequestException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class OrderLifecyclePolicy {

	public StaffOrderActions staffActions(OrderStatus status, @Nullable CancellationRequestStatus request) {
		return new StaffOrderActions(allowedTransitions(status, request),
				canCancel(ActorSide.RESTAURANT, status, request),
				List.copyOf(cancellationReasons(ActorSide.RESTAURANT, status, request)));
	}

	public CustomerOrderActions customerActions(OrderStatus status, @Nullable CancellationRequestStatus request) {
		Set<CancellationReason> reasons = EnumSet.noneOf(CancellationReason.class);
		reasons.addAll(cancellationReasons(ActorSide.CUSTOMER, status, request));
		reasons.addAll(requestReasons(status, request));
		return new CustomerOrderActions(canCancel(ActorSide.CUSTOMER, status, request),
				canRequestCancellation(status, request), List.copyOf(reasons));
	}

	public boolean hasPendingRequest(@Nullable CancellationRequestStatus request) {
		return request == CancellationRequestStatus.PENDING;
	}

	public void requireTransition(OrderStatus status, @Nullable CancellationRequestStatus request, OrderStatus target) {
		if (!Objects.equals(OrderConstants.NEXT_STATUS.get(status), target)) {
			throw new InvalidStatusTransitionException();
		}
		if (hasPendingRequest(request)) {
			throw new PendingCancellationRequestException();
		}
	}

	public void requireCancellation(ActorSide side, OrderStatus status, @Nullable CancellationRequestStatus request,
			CancellationReason reason) {
		Set<CancellationReason> allowed = Optional
			.ofNullable(OrderConstants.DIRECT_CANCELLATION_REASONS.get(side).get(status))
			.orElseThrow(CancellationNotAllowedException::new);
		if (hasPendingRequest(request)) {
			throw OrderConstants.PENDING_REQUEST_REFUSALS.get(side).get();
		}
		requireAllowedReason(allowed, reason);
	}

	public void requireCancellationRequest(OrderStatus status, @Nullable CancellationRequestStatus request,
			CancellationReason reason) {
		Set<CancellationReason> allowed = Optional.ofNullable(OrderConstants.REQUESTED_CANCELLATION_REASONS.get(status))
			.orElseThrow(CancellationNotAllowedException::new);
		if (request != null) {
			throw new CancellationAlreadyRequestedException();
		}
		requireAllowedReason(allowed, reason);
	}

	public void requireDecision(@Nullable CancellationRequestStatus request) {
		if (!hasPendingRequest(request)) {
			throw new NoPendingCancellationRequestException();
		}
	}

	private List<OrderStatus> allowedTransitions(OrderStatus status, @Nullable CancellationRequestStatus request) {
		if (hasPendingRequest(request)) {
			return List.of();
		}
		return Optional.ofNullable(OrderConstants.NEXT_STATUS.get(status)).stream().toList();
	}

	private boolean canCancel(ActorSide side, OrderStatus status, @Nullable CancellationRequestStatus request) {
		return !hasPendingRequest(request) && OrderConstants.DIRECT_CANCELLATION_REASONS.get(side).containsKey(status);
	}

	private Set<CancellationReason> cancellationReasons(ActorSide side, OrderStatus status,
			@Nullable CancellationRequestStatus request) {
		if (!canCancel(side, status, request)) {
			return Set.of();
		}
		return OrderConstants.DIRECT_CANCELLATION_REASONS.get(side).get(status);
	}

	private boolean canRequestCancellation(OrderStatus status, @Nullable CancellationRequestStatus request) {
		return request == null && OrderConstants.REQUESTED_CANCELLATION_REASONS.containsKey(status);
	}

	private Set<CancellationReason> requestReasons(OrderStatus status, @Nullable CancellationRequestStatus request) {
		if (!canRequestCancellation(status, request)) {
			return Set.of();
		}
		return OrderConstants.REQUESTED_CANCELLATION_REASONS.get(status);
	}

	private void requireAllowedReason(Set<CancellationReason> allowed, CancellationReason reason) {
		if (allowed.contains(reason)) {
			return;
		}
		throw new InvalidCancellationReasonException();
	}

}
