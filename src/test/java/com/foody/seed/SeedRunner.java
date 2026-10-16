package com.foody.seed;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.transaction.support.TransactionOperations;

@Slf4j
public class SeedRunner implements ApplicationRunner {

	private final List<Seeder> seeders;

	private final SeedProperties properties;

	private final TransactionOperations transactions;

	public SeedRunner(List<Seeder> seeders, SeedProperties properties, TransactionOperations transactions) {
		this.seeders = seeders;
		this.properties = properties;
		this.transactions = transactions;
	}

	@Override
	public void run(ApplicationArguments arguments) {
		seeders.forEach(this::runSeeder);
	}

	private void runSeeder(Seeder seeder) {
		String name = seeder.getClass().getSimpleName();
		if (seeder.alreadySeeded()) {
			log.info(SeedConstants.SKIPPED_LOG, name);
			return;
		}
		Instant start = Instant.now();
		transactions.executeWithoutResult(status -> seeder.seed(properties.scale()));
		log.info(SeedConstants.SEEDED_LOG, name, properties.scale(), Duration.between(start, Instant.now()).toMillis());
	}

}
