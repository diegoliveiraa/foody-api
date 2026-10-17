package com.foody.api.user.repository;

import com.foody.api.shared.security.Role;
import com.foody.api.user.dto.AdminUserFilter;
import com.foody.api.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

	@Query("select u from User u where lower(u.email) = lower(:email)")
	Optional<User> findByEmail(String email);

	@Query("select count(u) > 0 from User u where lower(u.email) = lower(:email)")
	boolean existsByEmail(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.role = :role and u.disabledAt is null order by u.id")
	List<User> lockActiveByRole(Role role);

	default Page<User> findAllMatching(AdminUserFilter filter, Pageable pageable) {
		return findAll(UserSpecifications.matching(filter), pageable);
	}

}
