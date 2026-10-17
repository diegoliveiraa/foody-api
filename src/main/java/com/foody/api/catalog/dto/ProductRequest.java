package com.foody.api.catalog.dto;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.shared.money.MoneyConstants;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

public record ProductRequest(
		@NotBlank @Size(min = CatalogConstants.PRODUCT_NAME_MIN_LENGTH,
				max = CatalogConstants.PRODUCT_NAME_MAX_LENGTH) String name,
		@Nullable @Size(max = CatalogConstants.DESCRIPTION_MAX_LENGTH) String description,
		@NotNull @DecimalMin(value = CatalogConstants.PRICE_EXCLUSIVE_MIN,
				inclusive = false) @DecimalMax(CatalogConstants.PRICE_MAX) @Digits(
						integer = CatalogConstants.PRICE_INTEGER_DIGITS,
						fraction = MoneyConstants.SCALE) BigDecimal price,
		@NotNull UUID categoryId) {

	public ProductRequest {
		name = StringUtils.trimWhitespace(name);
		description = StringUtils.hasText(description) ? StringUtils.trimWhitespace(description) : null;
	}

}
