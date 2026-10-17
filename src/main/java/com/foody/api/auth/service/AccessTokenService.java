package com.foody.api.auth.service;

import com.foody.api.auth.dto.AccessToken;
import com.foody.api.shared.security.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AccessTokenService {

	private final JwtEncoder encoder;

	private final JwtProperties properties;

	private final Clock clock;

	public AccessTokenService(JwtEncoder encoder, JwtProperties properties, Clock clock) {
		this.encoder = encoder;
		this.properties = properties;
		this.clock = clock;
	}

	public AccessToken issue(UUID userId) {
		Instant issuedAt = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(userId.toString())
			.issuedAt(issuedAt)
			.expiresAt(issuedAt.plus(properties.accessTokenTtl()))
			.id(UUID.randomUUID().toString())
			.build();
		String value = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
		return new AccessToken(value, properties.accessTokenTtl().toSeconds());
	}

}
