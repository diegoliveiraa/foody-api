package com.foody.api.shared.persistence;

import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class ActivatableEntity extends BaseEntity {

	private Instant disabledAt;

	public boolean isActive() {
		return disabledAt == null;
	}

	public void disable() {
		if (!isActive()) {
			return;
		}
		disabledAt = Instant.now();
	}

	public void enable() {
		disabledAt = null;
	}

}
