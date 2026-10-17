package com.foody.api.shared.persistence;

public enum ActivationStatus {

	ACTIVE, DISABLED;

	public static ActivationStatus of(ActivatableEntity entity) {
		return entity.isActive() ? ACTIVE : DISABLED;
	}

}
