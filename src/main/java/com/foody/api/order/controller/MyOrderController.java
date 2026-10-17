package com.foody.api.order.controller;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.dto.CancelOrderRequest;
import com.foody.api.order.dto.CustomerOrderDetailResponse;
import com.foody.api.order.dto.CustomerOrderSummaryResponse;
import com.foody.api.order.dto.PlaceOrderRequest;
import com.foody.api.order.service.CustomerOrderService;
import com.foody.api.shared.pagination.PageResponse;
import com.foody.api.shared.pagination.SortGuard;
import com.foody.api.shared.route.RouteConstants;
import com.foody.api.shared.security.Action;
import com.foody.api.shared.security.AuthenticatedUser;
import com.foody.api.shared.security.RequiresGrant;
import com.foody.api.shared.security.RequiresScreen;
import com.foody.api.shared.security.Screen;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RouteConstants.MY_ORDERS_PATH)
public class MyOrderController {

	private final CustomerOrderService orders;

	public MyOrderController(CustomerOrderService orders) {
		this.orders = orders;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresGrant(screen = Screen.STORE_MENU, action = Action.PLACE_ORDER)
	public CustomerOrderDetailResponse place(@AuthenticationPrincipal AuthenticatedUser caller,
			@Valid @RequestBody PlaceOrderRequest request) {
		return orders.place(caller.id(), request);
	}

	@GetMapping
	@RequiresScreen(Screen.STORE_MY_ORDERS)
	public PageResponse<CustomerOrderSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser caller,
			Pageable pageable) {
		Pageable allowed = SortGuard.requireAllowed(pageable, OrderConstants.SORTABLE_PROPERTIES);
		return PageResponse.of(orders.list(caller.id(), allowed));
	}

	@GetMapping(RouteConstants.ID_PATH)
	@RequiresScreen(Screen.STORE_MY_ORDERS)
	public CustomerOrderDetailResponse detail(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id) {
		return orders.detail(caller.id(), id);
	}

	@PostMapping(RouteConstants.CANCEL_PATH)
	@RequiresGrant(screen = Screen.STORE_MY_ORDERS, action = Action.CANCEL)
	public CustomerOrderDetailResponse cancel(@AuthenticationPrincipal AuthenticatedUser caller, @PathVariable UUID id,
			@Valid @RequestBody CancelOrderRequest request) {
		return orders.cancel(caller.id(), id, request);
	}

	@PostMapping(RouteConstants.CANCELLATION_REQUEST_PATH)
	@RequiresGrant(screen = Screen.STORE_MY_ORDERS, action = Action.REQUEST_CANCELLATION)
	public CustomerOrderDetailResponse requestCancellation(@AuthenticationPrincipal AuthenticatedUser caller,
			@PathVariable UUID id, @Valid @RequestBody CancelOrderRequest request) {
		return orders.requestCancellation(caller.id(), id, request);
	}

}
