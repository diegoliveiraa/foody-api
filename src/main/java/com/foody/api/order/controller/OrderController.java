package com.foody.api.order.controller;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.dto.StaffOrderDetailResponse;
import com.foody.api.order.dto.StaffOrderFilter;
import com.foody.api.order.dto.StaffOrderSummaryResponse;
import com.foody.api.order.entity.OrderStatus;
import com.foody.api.order.service.StaffOrderService;
import com.foody.api.shared.pagination.PageResponse;
import com.foody.api.shared.pagination.SortGuard;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.shared.security.RequiresScreen;
import com.foody.api.shared.security.Screen;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OrderConstants.ORDERS_PATH)
public class OrderController {

	private final StaffOrderService orders;

	public OrderController(StaffOrderService orders) {
		this.orders = orders;
	}

	@GetMapping
	@RequiresScreen(Screen.ORDERS_ORDER_LIST)
	public PageResponse<StaffOrderSummaryResponse> list(@RequestParam(required = false) List<OrderStatus> status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) String search, Pageable pageable) {
		Pageable allowed = SortGuard.requireAllowed(pageable, OrderConstants.SORTABLE_PROPERTIES);
		return PageResponse.of(orders.list(new StaffOrderFilter(status, from, to, search), allowed));
	}

	@GetMapping(RouteConstants.ID_PATH)
	@RequiresScreen(Screen.ORDERS_ORDER_LIST)
	public StaffOrderDetailResponse detail(@PathVariable UUID id) {
		return orders.detail(id);
	}

}
