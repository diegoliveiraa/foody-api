package com.foody.api.user.dto;

import com.foody.api.shared.security.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {
}
