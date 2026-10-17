package com.foody.api.user.dto;

import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record AdminUserResponse(UUID id, String name, String email, Role role, UserStatus status, Instant createdAt,
		@Nullable Instant disabledAt) {

	public static AdminUserResponse of(User user) {
		UserStatus status = user.isActive() ? UserStatus.ACTIVE : UserStatus.DISABLED;
		return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), status,
				user.getCreatedAt(), user.getDisabledAt());
	}

}
