package com.foody.api.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAddress {

	@Column(name = "delivery_zip_code", nullable = false, updatable = false)
	private String zipCode;

	@Column(name = "delivery_street", nullable = false, updatable = false)
	private String street;

	@Column(name = "delivery_number", nullable = false, updatable = false)
	private String number;

	@Column(name = "delivery_complement", updatable = false)
	private @Nullable String complement;

	@Column(name = "delivery_neighborhood", nullable = false, updatable = false)
	private String neighborhood;

	@Column(name = "delivery_city", nullable = false, updatable = false)
	private String city;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "delivery_state", nullable = false, updatable = false, length = 2)
	private BrazilianState state;

	public DeliveryAddress(String zipCode, String street, String number, @Nullable String complement,
			String neighborhood, String city, BrazilianState state) {
		this.zipCode = zipCode;
		this.street = street;
		this.number = number;
		this.complement = complement;
		this.neighborhood = neighborhood;
		this.city = city;
		this.state = state;
	}

}
