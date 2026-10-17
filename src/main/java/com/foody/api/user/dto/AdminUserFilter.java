package com.foody.api.user.dto;

import com.foody.api.shared.security.Role;
import org.jspecify.annotations.Nullable;

public record AdminUserFilter(@Nullable String search, @Nullable Role role, @Nullable UserStatus status) {
}
