package com.foody.api.shared.probe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ProbeRequest(@NotBlank String name, @Positive int quantity) {
}
