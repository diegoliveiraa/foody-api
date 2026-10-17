package com.foody.api.order.dto;

import com.foody.api.user.entity.User;
import java.util.UUID;

public record OrderCustomer(UUID id, String name, String email) {

	public static OrderCustomer of(User customer) {
		return new OrderCustomer(customer.getId(), customer.getName(), customer.getEmail());
	}

}
