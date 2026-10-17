package com.foody.testsupport.security;

import com.foody.api.shared.security.SecurityConstants;
import java.util.Map;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

record JwtKeysContextCustomizer(String publicKeyLocation, String privateKeyLocation) implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		TestPropertyValues.of(Map.of(SecurityConstants.JWT_PUBLIC_KEY_PROPERTY, publicKeyLocation,
				SecurityConstants.JWT_PRIVATE_KEY_PROPERTY, privateKeyLocation))
			.applyTo(context);
	}

}
