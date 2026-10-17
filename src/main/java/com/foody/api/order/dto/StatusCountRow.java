package com.foody.api.order.dto;

import com.foody.api.order.entity.OrderStatus;

public interface StatusCountRow {

	OrderStatus getStatus();

	long getOrderCount();

}
