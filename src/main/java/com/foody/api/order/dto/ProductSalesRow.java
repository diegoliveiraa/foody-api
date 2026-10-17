package com.foody.api.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductSalesRow {

	UUID getProductId();

	String getName();

	long getQuantity();

	BigDecimal getRevenue();

}
