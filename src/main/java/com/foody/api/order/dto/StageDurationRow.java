package com.foody.api.order.dto;

import com.foody.api.order.entity.OrderStatus;
import java.math.BigDecimal;

public interface StageDurationRow {

	OrderStatus getFromStatus();

	OrderStatus getToStatus();

	BigDecimal getAverageSeconds();

}
