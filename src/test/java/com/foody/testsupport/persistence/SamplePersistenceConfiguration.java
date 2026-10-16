package com.foody.testsupport.persistence;

import com.foody.api.FoodyApiApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration(proxyBeanMethods = false)
@EntityScan(basePackageClasses = { FoodyApiApplication.class, SamplePersistenceConfiguration.class })
@EnableJpaRepositories(basePackageClasses = SamplePersistenceConfiguration.class)
public class SamplePersistenceConfiguration {

	public static final String FLYWAY_LOCATIONS = "spring.flyway.locations=classpath:db/migration,classpath:db/testsupport";

}
