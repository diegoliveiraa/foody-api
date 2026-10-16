package com.foody.seed;

import java.util.Random;
import net.datafaker.Faker;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SeedProperties.class)
@ComponentScan(basePackageClasses = FactoryConfiguration.class, useDefaultFilters = false,
		includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EntityFactory.class),
		excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = SeedConstants.TEST_CLASS_PATTERN))
public class FactoryConfiguration {

	@Bean
	Faker faker(SeedProperties properties) {
		return new Faker(SeedConstants.LOCALE, new Random(properties.randomSeed()));
	}

}
