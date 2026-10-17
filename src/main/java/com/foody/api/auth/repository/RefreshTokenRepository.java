package com.foody.api.auth.repository;

import com.foody.api.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	@Query("select t from RefreshToken t join fetch t.user where t.tokenHash = :tokenHash")
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findByFamilyIdAndRevokedAtIsNull(UUID familyId);

	@Modifying
	@Query("delete from RefreshToken t where t.familyExpiresAt < :instant")
	int deleteFamiliesExpiredBefore(Instant instant);

}
