package com.foody.api;

import com.foody.seed.SeedConfiguration;
import com.foody.seed.SeedConstants;
import org.springframework.boot.SpringApplication;

public class TestFoodyApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(FoodyApiApplication::main)
				.with(SeedConfiguration.class)
				.withAdditionalProfiles(SeedConstants.PROFILE)
				.run(args);
	}

}
