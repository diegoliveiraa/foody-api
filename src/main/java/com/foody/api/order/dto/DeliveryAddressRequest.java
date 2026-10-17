package com.foody.api.order.dto;

import com.foody.api.order.OrderConstants;
import com.foody.api.order.entity.BrazilianState;
import com.foody.api.order.entity.DeliveryAddress;
import com.foody.api.order.util.ZipCodeUtils;
import com.foody.api.order.validation.ZipCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

public record DeliveryAddressRequest(@NotBlank @ZipCode String zipCode,
		@NotBlank @Size(min = OrderConstants.STREET_MIN_LENGTH, max = OrderConstants.STREET_MAX_LENGTH) String street,
		@NotBlank @Size(min = OrderConstants.ADDRESS_NUMBER_MIN_LENGTH,
				max = OrderConstants.ADDRESS_NUMBER_MAX_LENGTH) String number,
		@Nullable @Size(max = OrderConstants.COMPLEMENT_MAX_LENGTH) String complement,
		@NotBlank @Size(min = OrderConstants.NEIGHBORHOOD_MIN_LENGTH,
				max = OrderConstants.NEIGHBORHOOD_MAX_LENGTH) String neighborhood,
		@NotBlank @Size(min = OrderConstants.CITY_MIN_LENGTH, max = OrderConstants.CITY_MAX_LENGTH) String city,
		@NotNull BrazilianState state) {

	public DeliveryAddressRequest {
		zipCode = StringUtils.trimWhitespace(zipCode);
		street = StringUtils.trimWhitespace(street);
		number = StringUtils.trimWhitespace(number);
		complement = StringUtils.hasText(complement) ? StringUtils.trimWhitespace(complement) : null;
		neighborhood = StringUtils.trimWhitespace(neighborhood);
		city = StringUtils.trimWhitespace(city);
	}

	public DeliveryAddress toDeliveryAddress() {
		return new DeliveryAddress(ZipCodeUtils.normalize(zipCode), street, number, complement, neighborhood, city,
				state);
	}

}
