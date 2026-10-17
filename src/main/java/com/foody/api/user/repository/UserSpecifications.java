package com.foody.api.user.repository;

import com.foody.api.shared.security.Role;
import com.foody.api.shared.util.LikeUtils;
import com.foody.api.user.UserConstants;
import com.foody.api.user.dto.AdminUserFilter;
import com.foody.api.user.dto.UserStatus;
import com.foody.api.user.entity.User;
import java.util.List;
import java.util.Locale;
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
		return Optional.ofNullable(search)
			.map(String::strip)
			.filter(term -> !term.isEmpty())
			.map(term -> LikeUtils.containing(term.toLowerCase(Locale.ROOT)))
			.map(UserSpecifications::nameOrEmailLike);
	}

	private static PredicateSpecification<User> nameOrEmailLike(String pattern) {
		return (from, builder) -> builder.or(
				builder.like(builder.lower(from.get(UserConstants.NAME_ATTRIBUTE)), pattern, LikeUtils.ESCAPE),
				builder.like(builder.lower(from.get(UserConstants.EMAIL_ATTRIBUTE)), pattern, LikeUtils.ESCAPE));
	}

	private static Optional<PredicateSpecification<User>> withRole(@Nullable Role role) {
		return Optional.ofNullable(role).map(UserSpecifications::hasRole);
	}

	private static PredicateSpecification<User> hasRole(Role role) {
		return (from, builder) -> builder.equal(from.get(UserConstants.ROLE_ATTRIBUTE), role);
	}

	private static Optional<PredicateSpecification<User>> withStatus(@Nullable UserStatus status) {
		return Optional.ofNullable(status).map(UserSpecifications::hasStatus);
	}

	private static PredicateSpecification<User> hasStatus(UserStatus status) {
		return (from, builder) -> status == UserStatus.ACTIVE
				? builder.isNull(from.get(UserConstants.DISABLED_AT_ATTRIBUTE))
				: builder.isNotNull(from.get(UserConstants.DISABLED_AT_ATTRIBUTE));
	}

}
