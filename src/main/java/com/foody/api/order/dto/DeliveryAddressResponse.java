package com.foody.api.order.dto;

import com.foody.api.order.entity.BrazilianState;
import com.foody.api.order.entity.DeliveryAddress;
import org.jspecify.annotations.Nullable;

public record DeliveryAddressResponse(String zipCode, String street, String number, @Nullable String complement,
		String neighborhood, String city, BrazilianState state) {

	public static DeliveryAddressResponse of(DeliveryAddress address) {
		return new DeliveryAddressResponse(address.getZipCode(), address.getStreet(), address.getNumber(),
				address.getComplement(), address.getNeighborhood(), address.getCity(), address.getState());
	}

}
