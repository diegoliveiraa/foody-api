package com.foody.api.order.dto;

import java.math.BigDecimal;

public interface OrderTotalsRow {

	long getPlaced();

	long getDelivered();

	BigDecimal getRevenue();

	long getCancelled();

}
