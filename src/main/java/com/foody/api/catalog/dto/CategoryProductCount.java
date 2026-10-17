package com.foody.api.catalog.dto;

import java.util.UUID;

public record CategoryProductCount(UUID categoryId, long productCount) {
}
