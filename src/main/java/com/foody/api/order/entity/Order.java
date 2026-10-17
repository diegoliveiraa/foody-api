package com.foody.api.order.entity;

import com.foody.api.order.OrderConstants;
import com.foody.api.shared.persistence.BaseEntity;
import com.foody.api.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

	@Column(nullable = false, updatable = false)
	private long number;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(nullable = false, updatable = false)
	private User customer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status;

	@Column(nullable = false, updatable = false)
	private Instant placedAt;

	@Embedded
	private DeliveryAddress deliveryAddress;

	@Column(nullable = false, updatable = false)
	private BigDecimal total;

	@Enumerated(EnumType.STRING)
	private @Nullable ActorSide cancelledBy;

	@Enumerated(EnumType.STRING)
	private @Nullable CancellationReason cancelReason;

	private @Nullable String cancelNote;

	@OneToMany(mappedBy = OrderConstants.ORDER_ATTRIBUTE, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	@OneToMany(mappedBy = OrderConstants.ORDER_ATTRIBUTE, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderStatusHistory> history = new ArrayList<>();

	public Order(long number, User customer, DeliveryAddress deliveryAddress, BigDecimal total, Instant placedAt) {
		this.number = number;
		this.customer = customer;
		this.deliveryAddress = deliveryAddress;
		this.total = total;
		this.placedAt = placedAt;
		this.status = OrderStatus.RECEIVED;
		history.add(new OrderStatusHistory(this, null, OrderStatus.RECEIVED, customer, ActorSide.CUSTOMER, null, null,
				placedAt));
	}

	public void addItem(UUID productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {
		items.add(new OrderItem(this, productId, productName, unitPrice, quantity, subtotal));
	}

	public void advance(OrderStatus next, User actor, ActorSide side, Instant occurredAt) {
		history.add(new OrderStatusHistory(this, status, next, actor, side, null, null, occurredAt));
		status = next;
	}

	public void cancel(User actor, ActorSide side, CancellationReason reason, @Nullable String note,
			Instant occurredAt) {
		cancel(actor, side, side, reason, note, occurredAt);
	}

	public void cancel(User actor, ActorSide actorSide, ActorSide initiator, CancellationReason reason,
			@Nullable String note, Instant occurredAt) {
		history.add(new OrderStatusHistory(this, status, OrderStatus.CANCELLED, actor, actorSide, reason, note,
				occurredAt));
		status = OrderStatus.CANCELLED;
		cancelledBy = initiator;
		cancelReason = reason;
		cancelNote = note;
	}

	public boolean isPlacedBy(UUID customerId) {
		return customer.getId().equals(customerId);
	}

	public List<OrderItem> getItems() {
		return Collections.unmodifiableList(items);
	}

	public List<OrderStatusHistory> getHistory() {
		return Collections.unmodifiableList(history);
	}

}
