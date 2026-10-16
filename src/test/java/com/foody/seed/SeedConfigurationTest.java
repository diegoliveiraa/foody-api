package com.foody.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.FoodyApiApplication;
import com.foody.api.TestcontainersConfiguration;
import java.util.ArrayList;
import java.util.List;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = FoodyApiApplication.class, properties = "foody.seed.scale=LARGE")
@Import({ TestcontainersConfiguration.class, SeedConfiguration.class,
		SeedConfigurationTest.RecordingSeederConfiguration.class })
class SeedConfigurationTest {

	@Autowired
	private RecordingSeeder seeder;

	@Autowired
	private Faker faker;

	@Test
	void runsTheSeedersOfTheContextAtStartupWithTheConfiguredScale() {
		List<SeedScale> runs = seeder.runs();

		assertThat(runs).containsExactly(SeedScale.LARGE);
		assertThat(faker.name().firstName()).isNotBlank();
	}

	record RecordingSeeder(List<SeedScale> runs) implements Seeder {

		@Override
		public boolean alreadySeeded() {
			return false;
		}

		@Override
		public void seed(SeedScale scale) {
			runs.add(scale);
		}

	}

	@TestConfiguration(proxyBeanMethods = false)
	static class RecordingSeederConfiguration {

		@Bean
		RecordingSeeder recordingSeeder() {
			return new RecordingSeeder(new ArrayList<>());
		}

	}

}
