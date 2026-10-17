package com.foody.api.order.entity;

import com.foody.api.shared.persistence.BaseEntity;
import com.foody.api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Getter
@Entity
@Table(name = "order_status_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusHistory extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(nullable = false, updatable = false)
	private Order order;

	@Enumerated(EnumType.STRING)
	@Column(updatable = false)
	private @Nullable OrderStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private OrderStatus toStatus;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(nullable = false, updatable = false)
	private User actor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private ActorSide actorSide;

	@Enumerated(EnumType.STRING)
	@Column(updatable = false)
	private @Nullable CancellationReason reason;

	@Column(updatable = false)
	private @Nullable String note;

	@Column(nullable = false, updatable = false)
	private Instant occurredAt;

	OrderStatusHistory(Order order, @Nullable OrderStatus fromStatus, OrderStatus toStatus, User actor,
			ActorSide actorSide, @Nullable CancellationReason reason, @Nullable String note, Instant occurredAt) {
		this.order = order;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.actor = actor;
		this.actorSide = actorSide;
		this.reason = reason;
		this.note = note;
		this.occurredAt = occurredAt;
	}

}
