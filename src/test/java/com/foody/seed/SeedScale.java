package com.foody.seed;

public enum SeedScale {

	SMALL(1), MEDIUM(10), LARGE(100);

	private final int multiplier;

	SeedScale(int multiplier) {
		this.multiplier = multiplier;
	}

	public int of(int baseCount) {
		return baseCount * multiplier;
	}

}
