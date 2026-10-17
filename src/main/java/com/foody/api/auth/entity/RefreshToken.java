package com.foody.api.auth.entity;

import com.foody.api.shared.persistence.BaseEntity;
import com.foody.api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String tokenHash;

	@Column(nullable = false)
	private UUID familyId;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private Instant familyExpiresAt;

	private Instant revokedAt;

	public RefreshToken(User user, String tokenHash, UUID familyId, Instant expiresAt, Instant familyExpiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.familyId = familyId;
		this.expiresAt = expiresAt;
		this.familyExpiresAt = familyExpiresAt;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isUsableAt(Instant instant) {
		return !isRevoked() && instant.isBefore(expiresAt) && instant.isBefore(familyExpiresAt);
	}

	public void revoke(Instant instant) {
		if (isRevoked()) {
			return;
		}
		revokedAt = instant;
	}

}
