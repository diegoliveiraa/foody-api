package com.foody.api.order.entity;

public enum OrderStatus {

	RECEIVED(true), PREPARING(true), OUT_FOR_DELIVERY(true), DELIVERED(false), CANCELLED(false);

	private final boolean active;

	OrderStatus(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

}
