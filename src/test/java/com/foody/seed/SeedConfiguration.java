package com.foody.seed;

import com.foody.seed.user.IdentitySeedProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionOperations;

@Configuration(proxyBeanMethods = false)
@Import(FactoryConfiguration.class)
@EnableConfigurationProperties(IdentitySeedProperties.class)
@ComponentScan(basePackageClasses = SeedConfiguration.class, useDefaultFilters = false,
		includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Seeder.class),
		excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = SeedConstants.TEST_CLASS_PATTERN))
public class SeedConfiguration {

	@Bean
	SeedRunner seedRunner(ObjectProvider<Seeder> seeders, SeedProperties properties,
			TransactionOperations transactions) {
		return new SeedRunner(seeders.orderedStream().toList(), properties, transactions);
	}

}
