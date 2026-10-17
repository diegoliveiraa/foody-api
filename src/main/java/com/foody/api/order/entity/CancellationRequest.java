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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Getter
@Entity
@Table(name = "cancellation_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CancellationRequest extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(nullable = false, updatable = false, unique = true)
	private Order order;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private CancellationReason reason;

	@Column(updatable = false)
	private @Nullable String note;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CancellationRequestStatus status;

	@Column(nullable = false, updatable = false)
	private Instant requestedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "decided_by")
	private @Nullable User decidedBy;

	private @Nullable Instant decidedAt;

	private @Nullable String decisionNote;

	public CancellationRequest(Order order, CancellationReason reason, @Nullable String note, Instant requestedAt) {
		this.order = order;
		this.reason = reason;
		this.note = note;
		this.requestedAt = requestedAt;
		this.status = CancellationRequestStatus.PENDING;
	}

	public void approve(User decider, @Nullable String note, Instant occurredAt) {
		decide(CancellationRequestStatus.APPROVED, decider, note, occurredAt);
	}

	public void decline(User decider, @Nullable String note, Instant occurredAt) {
		decide(CancellationRequestStatus.DECLINED, decider, note, occurredAt);
	}

	private void decide(CancellationRequestStatus decision, User decider, @Nullable String note, Instant occurredAt) {
		status = decision;
		decidedBy = decider;
		decidedAt = occurredAt;
		decisionNote = note;
	}

}
