package com.foody.api.user.repository;

import com.foody.api.shared.persistence.ActivatableSpecifications;
import com.foody.api.shared.persistence.ActivationStatus;
import com.foody.api.shared.security.Role;
import com.foody.api.shared.util.TextSearchUtils;
import com.foody.api.user.UserConstants;
import com.foody.api.user.dto.AdminUserFilter;
import com.foody.api.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

	private UserSpecifications() {
	}

	public static Specification<User> matching(AdminUserFilter filter) {
		List<PredicateSpecification<User>> criteria = Stream
			.of(searching(filter.search()), withRole(filter.role()), withStatus(filter.status()))
			.flatMap(Optional::stream)
			.toList();
		return Specification.where(PredicateSpecification.allOf(criteria));
	}

	private static Optional<PredicateSpecification<User>> searching(@Nullable String search) {
		return TextSearchUtils.searchTerm(search).map(UserSpecifications::nameOrEmailLike);
	}

	private static PredicateSpecification<User> nameOrEmailLike(String term) {
		return (from, builder) -> builder.or(
				TextSearchUtils.matches(builder, from.get(UserConstants.NAME_ATTRIBUTE), term),
				TextSearchUtils.matches(builder, from.get(UserConstants.EMAIL_ATTRIBUTE), term));
	}

	private static Optional<PredicateSpecification<User>> withRole(@Nullable Role role) {
		return Optional.ofNullable(role).map(UserSpecifications::hasRole);
	}

	private static PredicateSpecification<User> hasRole(Role role) {
		return (from, builder) -> builder.equal(from.get(UserConstants.ROLE_ATTRIBUTE), role);
	}

	private static Optional<PredicateSpecification<User>> withStatus(@Nullable ActivationStatus status) {
		return Optional.ofNullable(status).map(ActivatableSpecifications::hasStatus);
	}

}
