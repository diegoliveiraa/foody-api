package com.foody.testsupport.api;

import com.foody.api.TestcontainersConfiguration;
import com.foody.seed.FactoryConfiguration;
import com.foody.testsupport.security.AccessTokens;
import com.foody.testsupport.security.AuthClient;
import com.foody.testsupport.security.GrantProbeController;
import com.foody.testsupport.time.MutableClock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
@Import({ TestcontainersConfiguration.class, FactoryConfiguration.class, GrantProbeController.class, AccessTokens.class,
		AuthClient.class })
public class ApiIntegrationConfiguration {

	@Bean
	@Primary
	MutableClock mutableClock() {
		return new MutableClock();
	}

}
