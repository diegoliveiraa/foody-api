package com.foody.seed;

import java.util.Locale;

public final class SeedConstants {

	public static final String PROFILE = "seed";

	public static final String PROPERTIES_PREFIX = "foody.seed";

	public static final Locale LOCALE = Locale.of("pt", "BR");

	public static final String SKIPPED_LOG = "Seed {} skipped: its data is already there";

	public static final String SEEDED_LOG = "Seed {} done at scale {} in {} ms";

	public static final String TEST_CLASS_PATTERN = ".*Test(\\$.*)?";

	public static final int PERCENT = 100;

	private SeedConstants() {
	}

}
