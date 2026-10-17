package com.foody.api.catalog.dto;

import com.foody.api.catalog.CatalogConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.util.StringUtils;

public record CategoryRequest(
		@NotBlank @Size(min = CatalogConstants.CATEGORY_NAME_MIN_LENGTH,
				max = CatalogConstants.CATEGORY_NAME_MAX_LENGTH) String name,
		@NotNull @Min(CatalogConstants.DISPLAY_ORDER_MIN) @Max(CatalogConstants.DISPLAY_ORDER_MAX) Integer displayOrder) {

	public CategoryRequest {
		name = StringUtils.trimWhitespace(name);
	}

}
