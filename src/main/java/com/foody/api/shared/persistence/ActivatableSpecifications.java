package com.foody.api.shared.persistence;

import org.springframework.data.jpa.domain.PredicateSpecification;

public final class ActivatableSpecifications {

	private ActivatableSpecifications() {
	}

	public static <T extends ActivatableEntity> PredicateSpecification<T> hasStatus(ActivationStatus status) {
		return (from, builder) -> status == ActivationStatus.ACTIVE
				? builder.isNull(from.get(PersistenceConstants.DISABLED_AT_ATTRIBUTE))
				: builder.isNotNull(from.get(PersistenceConstants.DISABLED_AT_ATTRIBUTE));
	}

}
