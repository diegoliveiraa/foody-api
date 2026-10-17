package com.foody.api.catalog.dto;

import com.foody.api.shared.persistence.ActivationStatus;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ProductFilter(@Nullable UUID categoryId, @Nullable ActivationStatus status, @Nullable String search) {
}
