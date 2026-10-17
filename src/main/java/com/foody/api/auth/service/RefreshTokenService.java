package com.foody.api.auth.service;

import com.foody.api.auth.AuthConstants;
import com.foody.api.auth.dto.IssuedRefreshToken;
import com.foody.api.auth.dto.RotatedRefreshToken;
import com.foody.api.auth.entity.RefreshToken;
import com.foody.api.auth.exception.RefreshTokenInvalidException;
import com.foody.api.auth.repository.RefreshTokenRepository;
import com.foody.api.shared.security.JwtProperties;
import com.foody.api.shared.util.HashUtils;
import com.foody.api.user.entity.User;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

	private final RefreshTokenRepository repository;

	private final JwtProperties properties;

	private final Clock clock;

	private final SecureRandom random = new SecureRandom();

	public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties, Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.clock = clock;
	}

	@Transactional
	public IssuedRefreshToken startFamily(User user) {
		Instant now = clock.instant();
		return issue(user, UUID.randomUUID(), now.plus(properties.refreshFamilyTtl()), now);
	}

	@Transactional(noRollbackFor = RefreshTokenInvalidException.class)
	public RotatedRefreshToken rotate(String presentedValue) {
		Instant now = clock.instant();
		RefreshToken presented = find(presentedValue).orElseThrow(RefreshTokenInvalidException::new);
		if (presented.isRevoked()) {
			revokeFamily(presented.getFamilyId(), now);
			throw new RefreshTokenInvalidException();
		}
		if (!presented.isUsableAt(now) || !presented.getUser().isActive()) {
			throw new RefreshTokenInvalidException();
		}
		presented.revoke(now);
		IssuedRefreshToken next = issue(presented.getUser(), presented.getFamilyId(), presented.getFamilyExpiresAt(),
				now);
		return new RotatedRefreshToken(presented.getUser().getId(), next);
	}

	@Transactional
	public void revokeFamilyOf(String presentedValue) {
		Instant now = clock.instant();
		find(presentedValue).ifPresent(token -> revokeFamily(token.getFamilyId(), now));
	}

	private Optional<RefreshToken> find(String presentedValue) {
		return Optional.ofNullable(presentedValue).map(HashUtils::sha256Hex).flatMap(repository::findByTokenHash);
	}

	private void revokeFamily(UUID familyId, Instant now) {
		repository.findByFamilyIdAndRevokedAtIsNull(familyId).forEach(token -> token.revoke(now));
	}

	private IssuedRefreshToken issue(User user, UUID familyId, Instant familyExpiresAt, Instant now) {
		String value = newTokenValue();
		Instant renewedUntil = now.plus(properties.refreshTokenTtl());
		Instant expiresAt = renewedUntil.isBefore(familyExpiresAt) ? renewedUntil : familyExpiresAt;
		repository.save(new RefreshToken(user, HashUtils.sha256Hex(value), familyId, expiresAt, familyExpiresAt));
		return new IssuedRefreshToken(value, Duration.between(now, expiresAt));
	}

	private String newTokenValue() {
		byte[] bytes = new byte[AuthConstants.REFRESH_TOKEN_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

}
