package com.foody.api.shared.security;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {

	@Bean
	JwtDecoder jwtDecoder(JwtProperties properties, Clock clock) {
		JwtTimestampValidator timestamps = new JwtTimestampValidator();
		timestamps.setClock(clock);
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(properties.publicKey()).build();
		decoder.setJwtValidator(
				JwtValidators.createDefaultWithValidators(timestamps, new JwtIssuerValidator(properties.issuer())));
		return decoder;
	}

	@Bean
	JwtEncoder jwtEncoder(JwtProperties properties) {
		return NimbusJwtEncoder.withKeyPair(properties.publicKey(), properties.privateKey()).build();
	}

}
