package com.foody.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.transaction.support.TransactionOperations;

class SeedRunnerTest {

	private static final long RANDOM_SEED = 42;

	@Test
	void runsEverySeederInOrderAtTheConfiguredScale() {
		List<String> calls = new ArrayList<>();
		List<Seeder> seeders = List.of(new RecordingSeeder("users", false, calls),
				new RecordingSeeder("products", false, calls));
		SeedRunner runner = new SeedRunner(seeders, new SeedProperties(SeedScale.MEDIUM, RANDOM_SEED),
				TransactionOperations.withoutTransaction());

		runner.run(new DefaultApplicationArguments());

		assertThat(calls).containsExactly("users:MEDIUM", "products:MEDIUM");
	}

	@Test
	void skipsASeederWhoseDataIsAlreadyThere() {
		List<String> calls = new ArrayList<>();
		List<Seeder> seeders = List.of(new RecordingSeeder("users", true, calls),
				new RecordingSeeder("products", false, calls));
		SeedRunner runner = new SeedRunner(seeders, new SeedProperties(SeedScale.SMALL, RANDOM_SEED),
				TransactionOperations.withoutTransaction());

		runner.run(new DefaultApplicationArguments());

		assertThat(calls).containsExactly("products:SMALL");
	}

	private record RecordingSeeder(String name, boolean alreadySeeded, List<String> calls) implements Seeder {

		@Override
		public void seed(SeedScale scale) {
			calls.add(name + ":" + scale);
		}

	}

}
