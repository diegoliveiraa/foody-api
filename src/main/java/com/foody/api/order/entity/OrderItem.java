package com.foody.api.order.entity;

import com.foody.api.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(nullable = false, updatable = false)
	private Order order;

	@Column(nullable = false, updatable = false)
	private UUID productId;

	@Column(nullable = false, updatable = false)
	private String productName;

	@Column(nullable = false, updatable = false)
	private BigDecimal unitPrice;

	@Column(nullable = false, updatable = false)
	private int quantity;

	@Column(nullable = false, updatable = false)
	private BigDecimal subtotal;

	OrderItem(Order order, UUID productId, String productName, BigDecimal unitPrice, int quantity,
			BigDecimal subtotal) {
		this.order = order;
		this.productId = productId;
		this.productName = productName;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
		this.subtotal = subtotal;
	}

}
