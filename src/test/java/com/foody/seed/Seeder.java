package com.foody.seed;

public interface Seeder {

	boolean alreadySeeded();

	void seed(SeedScale scale);

}
