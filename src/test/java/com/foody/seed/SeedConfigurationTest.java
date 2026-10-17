package com.foody.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.FoodyApiApplication;
import com.foody.api.TestcontainersConfiguration;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = FoodyApiApplication.class,
		properties = { "foody.seed.scale=MEDIUM", "foody.seed.admin.email=admin.config@foody.test",
				"foody.seed.admin.password=admin-config-pass-1", "foody.seed.staff.email=staff.config@foody.test",
				"foody.seed.customer.email=customer.config@foody.test",
				"foody.seed.default-password=default-config-pass-1" })
@Import({ TestcontainersConfiguration.class, SeedConfiguration.class,
		SeedConfigurationTest.RecordingSeederConfiguration.class })
class SeedConfigurationTest {

	@Autowired
	private RecordingSeeder seeder;

	@Autowired
	private Faker faker;

	@Autowired
	private UserRepository userRepository;

	@Test
	void runsTheSeedersOfTheContextAtStartupWithTheConfiguredScale() {
		List<SeedScale> runs = seeder.runs();

		assertThat(runs).containsExactly(SeedScale.MEDIUM);
		assertThat(faker.name().firstName()).isNotBlank();
		assertThat(userRepository.findByEmail("admin.config@foody.test")).map(User::getRole).contains(Role.ADMIN);
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
