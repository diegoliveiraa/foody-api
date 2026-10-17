package com.foody.api.shared.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = SecurityConstants.JWT_PROPERTIES_PREFIX)
public record JwtProperties(@NotBlank @DefaultValue(SecurityConstants.DEFAULT_ISSUER) String issuer,
		@NotNull @DefaultValue(SecurityConstants.DEFAULT_ACCESS_TOKEN_TTL) Duration accessTokenTtl,
		@NotNull @DefaultValue(SecurityConstants.DEFAULT_REFRESH_TOKEN_TTL) Duration refreshTokenTtl,
		@NotNull @DefaultValue(SecurityConstants.DEFAULT_REFRESH_FAMILY_TTL) Duration refreshFamilyTtl,
		@NotNull RSAPublicKey publicKey, @NotNull RSAPrivateKey privateKey) {
}
