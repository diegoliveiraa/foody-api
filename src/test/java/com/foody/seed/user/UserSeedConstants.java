package com.foody.seed.user;

import java.util.List;
import java.util.regex.Pattern;

public final class UserSeedConstants {

	public static final List<String> EMAIL_DOMAINS = List.of("gmail.com", "hotmail.com", "outlook.com", "yahoo.com.br",
			"uol.com.br");

	public static final Pattern NON_EMAIL_LETTERS = Pattern.compile("[^a-z]");

	public static final String NAME_SEPARATOR = " ";

	public static final String EMAIL_DOMAIN_SEPARATOR = "@";

	public static final String EMAIL_PART_SEPARATOR = ".";

	public static final int EXTRA_STAFF_COUNT = 4;

	public static final int CUSTOMER_BASE_COUNT = 50;

	public static final int DISABLED_CUSTOMER_PERCENT = 5;

	private UserSeedConstants() {
	}

}
