package com.foody.api.user.dto;

import com.foody.api.shared.persistence.ActivationStatus;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record AdminUserResponse(UUID id, String name, String email, Role role, ActivationStatus status,
		Instant createdAt, @Nullable Instant disabledAt) {

	public static AdminUserResponse of(User user) {
		return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
				ActivationStatus.of(user), user.getCreatedAt(), user.getDisabledAt());
	}

}
