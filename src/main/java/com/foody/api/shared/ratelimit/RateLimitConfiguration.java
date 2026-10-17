package com.foody.api.shared.ratelimit;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration {

	@Bean
	RateLimiter rateLimiter(RateLimitProperties properties, Clock clock) {
		return new RateLimiter(properties, new ClockTimeMeter(clock));
	}

}
