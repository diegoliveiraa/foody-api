package com.foody.api.order.dto;

import com.foody.api.order.entity.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record StaffOrderFilter(@Nullable List<OrderStatus> statuses, @Nullable LocalDate from, @Nullable LocalDate to,
		@Nullable String search) {
}
